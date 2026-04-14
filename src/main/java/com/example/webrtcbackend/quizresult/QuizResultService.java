package com.example.webrtcbackend.quizresult;

import com.example.webrtcbackend.common.NotFoundException;
import com.example.webrtcbackend.quizresult.dto.*;
import com.example.webrtcbackend.quizresult.dto.QuizResultResponse.AnswerDetailResponse;
import com.example.webrtcbackend.quizresult.dto.UserStatsResponse.ProgressPoint;
import com.example.webrtcbackend.quizresult.dto.AssignmentStatsResponse.QuestionAccuracy;
import com.example.webrtcbackend.transcription.Assignment;
import com.example.webrtcbackend.transcription.AssignmentRepository;
import com.example.webrtcbackend.transcription.Question;
import com.example.webrtcbackend.transcription.QuestionRepository;
import com.example.webrtcbackend.user.User;
import com.example.webrtcbackend.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class QuizResultService {

    private static final Logger log = LoggerFactory.getLogger(QuizResultService.class);

    private final QuizResultRepository quizResultRepository;
    private final QuizAnswerDetailRepository answerDetailRepository;
    private final AssignmentRepository assignmentRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;

    public QuizResultService(QuizResultRepository quizResultRepository,
                             QuizAnswerDetailRepository answerDetailRepository,
                             AssignmentRepository assignmentRepository,
                             QuestionRepository questionRepository,
                             UserRepository userRepository) {
        this.quizResultRepository = quizResultRepository;
        this.answerDetailRepository = answerDetailRepository;
        this.assignmentRepository = assignmentRepository;
        this.questionRepository = questionRepository;
        this.userRepository = userRepository;
    }

    // ========================================================================
    // 1. Submit quiz result
    // ========================================================================

    @Transactional
    public QuizResultResponse submitQuiz(SubmitQuizRequest request) {
        log.info("Submitting quiz: userId={}, assignmentId={}, answers={}",
                request.getUserId(), request.getAssignmentId(), request.getAnswers().size());

        // Validate assignment exists
        Assignment assignment = assignmentRepository.findById(request.getAssignmentId())
                .orElseThrow(() -> new NotFoundException("Assignment not found with id=" + request.getAssignmentId()));

        // Validate user exists
        userRepository.findById(request.getUserId())
                .orElseThrow(() -> new NotFoundException("User not found with id=" + request.getUserId()));

        // Load all questions for this assignment to validate and grade
        List<Question> questions = questionRepository.findByAssignmentId(request.getAssignmentId());
        Map<Long, Question> questionMap = questions.stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));

        // Grade each answer
        int correctCount = 0;
        int wrongCount = 0;
        List<QuizAnswerDetail> details = new ArrayList<>();

        for (SubmitQuizRequest.AnswerItem item : request.getAnswers()) {
            Question question = questionMap.get(item.getQuestionId());
            if (question == null) {
                log.warn("Question not found: questionId={}, skipping", item.getQuestionId());
                continue;
            }

            boolean isCorrect = question.getCorrectAnswer() != null
                    && question.getCorrectAnswer().equalsIgnoreCase(item.getSelectedAnswer());

            if (isCorrect) {
                correctCount++;
            } else {
                wrongCount++;
            }

            QuizAnswerDetail detail = new QuizAnswerDetail();
            detail.setQuestionId(item.getQuestionId());
            detail.setSelectedAnswer(item.getSelectedAnswer());
            detail.setCorrectAnswer(question.getCorrectAnswer());
            detail.setIsCorrect(isCorrect);
            details.add(detail);
        }

        int totalQuestions = details.size();
        double score = totalQuestions > 0 ? Math.round((double) correctCount / totalQuestions * 100.0 * 100.0) / 100.0 : 0.0;

        // Create QuizResult entity
        QuizResult result = new QuizResult();
        result.setUserId(request.getUserId());
        result.setAssignmentId(request.getAssignmentId());
        result.setTotalQuestions(totalQuestions);
        result.setCorrectCount(correctCount);
        result.setWrongCount(wrongCount);
        result.setScore(score);
        result.setDurationSeconds(request.getDurationSeconds());
        result.setCompletedAt(Instant.now());

        // Link details to result
        for (QuizAnswerDetail detail : details) {
            detail.setQuizResult(result);
        }
        result.setAnswerDetails(details);

        result = quizResultRepository.save(result);
        log.info("Quiz result saved: id={}, score={}, correct={}/{}", result.getId(), score, correctCount, totalQuestions);

        return toResponse(result, assignment.getTitle(), questionMap);
    }

    // ========================================================================
    // 2. Get quiz result by ID
    // ========================================================================

    @Transactional(readOnly = true)
    public QuizResultResponse getResultById(Long resultId) {
        QuizResult result = quizResultRepository.findById(resultId)
                .orElseThrow(() -> new NotFoundException("Quiz result not found with id=" + resultId));

        Assignment assignment = assignmentRepository.findById(result.getAssignmentId())
                .orElseThrow(() -> new NotFoundException("Assignment not found with id=" + result.getAssignmentId()));

        Map<Long, Question> questionMap = loadQuestionMap(result.getAssignmentId());
        return toResponse(result, assignment.getTitle(), questionMap);
    }

    // ========================================================================
    // 3. Get history for a user on a specific assignment
    // ========================================================================

    @Transactional(readOnly = true)
    public List<QuizResultResponse> getHistory(Long userId, Long assignmentId) {
        List<QuizResult> results = quizResultRepository
                .findByUserIdAndAssignmentIdOrderByCompletedAtDesc(userId, assignmentId);

        if (results.isEmpty()) {
            return Collections.emptyList();
        }

        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new NotFoundException("Assignment not found with id=" + assignmentId));

        Map<Long, Question> questionMap = loadQuestionMap(assignmentId);

        return results.stream()
                .map(r -> toResponse(r, assignment.getTitle(), questionMap))
                .toList();
    }

    // ========================================================================
    // 4. Get the latest result for a user (quick access)
    // ========================================================================

    @Transactional(readOnly = true)
    public QuizResultResponse getLatestResult(Long userId) {
        QuizResult result = quizResultRepository.findFirstByUserIdOrderByCompletedAtDesc(userId)
                .orElseThrow(() -> new NotFoundException("No quiz results found for userId=" + userId));

        Assignment assignment = assignmentRepository.findById(result.getAssignmentId())
                .orElseThrow(() -> new NotFoundException("Assignment not found with id=" + result.getAssignmentId()));

        Map<Long, Question> questionMap = loadQuestionMap(result.getAssignmentId());
        return toResponse(result, assignment.getTitle(), questionMap);
    }

    // ========================================================================
    // 5. Get latest result for a user on a specific assignment
    // ========================================================================

    @Transactional(readOnly = true)
    public QuizResultResponse getLatestResultForAssignment(Long userId, Long assignmentId) {
        QuizResult result = quizResultRepository
                .findFirstByUserIdAndAssignmentIdOrderByCompletedAtDesc(userId, assignmentId)
                .orElseThrow(() -> new NotFoundException(
                        "No quiz results found for userId=" + userId + " and assignmentId=" + assignmentId));

        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new NotFoundException("Assignment not found with id=" + assignmentId));

        Map<Long, Question> questionMap = loadQuestionMap(assignmentId);
        return toResponse(result, assignment.getTitle(), questionMap);
    }

    // ========================================================================
    // 6. User statistics (overall)
    // ========================================================================

    @Transactional(readOnly = true)
    public UserStatsResponse getUserStats(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with id=" + userId));

        long totalAttempts = quizResultRepository.countByUserId(userId);
        Double avgScore = quizResultRepository.findAverageScoreByUserId(userId);
        Double maxScore = quizResultRepository.findMaxScoreByUserId(userId);
        Double minScore = quizResultRepository.findMinScoreByUserId(userId);

        // Build progress history (all results ordered by time)
        List<QuizResult> allResults = quizResultRepository.findByUserIdOrderByCompletedAtDesc(userId);

        // Load assignment titles in bulk
        Set<Long> assignmentIds = allResults.stream()
                .map(QuizResult::getAssignmentId)
                .collect(Collectors.toSet());
        Map<Long, String> assignmentTitles = assignmentRepository.findAllById(assignmentIds).stream()
                .collect(Collectors.toMap(Assignment::getId, Assignment::getTitle));

        List<ProgressPoint> progressHistory = allResults.stream()
                .map(r -> {
                    ProgressPoint p = new ProgressPoint();
                    p.setResultId(r.getId());
                    p.setAssignmentId(r.getAssignmentId());
                    p.setAssignmentTitle(assignmentTitles.getOrDefault(r.getAssignmentId(), "Unknown"));
                    p.setScore(r.getScore());
                    p.setCorrectCount(r.getCorrectCount());
                    p.setTotalQuestions(r.getTotalQuestions());
                    p.setCompletedAt(r.getCompletedAt().toString());
                    return p;
                })
                .toList();

        UserStatsResponse stats = new UserStatsResponse();
        stats.setUserId(userId);
        stats.setUsername(user.getUsername());
        stats.setTotalAttempts(totalAttempts);
        stats.setAverageScore(avgScore != null ? Math.round(avgScore * 100.0) / 100.0 : null);
        stats.setHighestScore(maxScore);
        stats.setLowestScore(minScore);
        stats.setProgressHistory(progressHistory);
        return stats;
    }

    // ========================================================================
    // 7. Assignment statistics (with per-question accuracy)
    // ========================================================================

    @Transactional(readOnly = true)
    public AssignmentStatsResponse getAssignmentStats(Long assignmentId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new NotFoundException("Assignment not found with id=" + assignmentId));

        long totalAttempts = quizResultRepository.countByAssignmentId(assignmentId);

        // Calculate average score from the stats query
        List<Object[]> statsRows = quizResultRepository.findAssignmentStats(assignmentId);
        Double avgScore = null;
        if (!statsRows.isEmpty()) {
            avgScore = (Double) statsRows.get(0)[1];
        }

        // Per-question accuracy
        List<Object[]> questionAccuracyRows = answerDetailRepository.findQuestionAccuracyByAssignment(assignmentId);

        // Load questions for text
        Map<Long, Question> questionMap = loadQuestionMap(assignmentId);

        List<QuestionAccuracy> accuracies = questionAccuracyRows.stream()
                .map(row -> {
                    Long questionId = (Long) row[0];
                    long attempts = (Long) row[1];
                    long correct = (Long) row[2];
                    long wrong = attempts - correct;

                    QuestionAccuracy qa = new QuestionAccuracy();
                    qa.setQuestionId(questionId);
                    qa.setQuestionText(questionMap.containsKey(questionId)
                            ? questionMap.get(questionId).getQuestion() : "Unknown");
                    qa.setTotalAttempts(attempts);
                    qa.setCorrectCount(correct);
                    qa.setWrongCount(wrong);
                    qa.setAccuracyRate(attempts > 0 ? Math.round((double) correct / attempts * 100.0 * 100.0) / 100.0 : 0.0);
                    return qa;
                })
                .toList();

        AssignmentStatsResponse response = new AssignmentStatsResponse();
        response.setAssignmentId(assignmentId);
        response.setAssignmentTitle(assignment.getTitle());
        response.setTotalAttempts(totalAttempts);
        response.setAverageScore(avgScore != null ? Math.round(avgScore * 100.0) / 100.0 : null);
        response.setQuestionAccuracies(accuracies);
        return response;
    }

    // ========================================================================
    // 8. User progress within a time range
    // ========================================================================

    @Transactional(readOnly = true)
    public List<QuizResultResponse> getUserProgressByDateRange(Long userId, Instant from, Instant to) {
        List<QuizResult> results = quizResultRepository
                .findByUserIdAndCompletedAtBetweenOrderByCompletedAtAsc(userId, from, to);

        // Load assignment titles in bulk
        Set<Long> assignmentIds = results.stream()
                .map(QuizResult::getAssignmentId)
                .collect(Collectors.toSet());
        Map<Long, String> assignmentTitles = assignmentRepository.findAllById(assignmentIds).stream()
                .collect(Collectors.toMap(Assignment::getId, Assignment::getTitle));

        // Load question maps for all assignments
        Map<Long, Map<Long, Question>> questionMaps = new HashMap<>();
        for (Long aId : assignmentIds) {
            questionMaps.put(aId, loadQuestionMap(aId));
        }

        return results.stream()
                .map(r -> toResponse(r, assignmentTitles.getOrDefault(r.getAssignmentId(), "Unknown"),
                        questionMaps.getOrDefault(r.getAssignmentId(), Collections.emptyMap())))
                .toList();
    }

    // ========================================================================
    // 9. Get all results for an assignment (all users)
    // ========================================================================

    @Transactional(readOnly = true)
    public List<QuizResultResponse> getResultsByAssignment(Long assignmentId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new NotFoundException("Assignment not found with id=" + assignmentId));

        List<QuizResult> results = quizResultRepository.findByAssignmentIdOrderByCompletedAtDesc(assignmentId);
        Map<Long, Question> questionMap = loadQuestionMap(assignmentId);

        return results.stream()
                .map(r -> toResponse(r, assignment.getTitle(), questionMap))
                .toList();
    }

    // ========================================================================
    // Helper methods
    // ========================================================================

    private Map<Long, Question> loadQuestionMap(Long assignmentId) {
        return questionRepository.findByAssignmentId(assignmentId).stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));
    }

    private QuizResultResponse toResponse(QuizResult result, String assignmentTitle, Map<Long, Question> questionMap) {
        QuizResultResponse response = new QuizResultResponse();
        response.setId(result.getId());
        response.setUserId(result.getUserId());
        response.setAssignmentId(result.getAssignmentId());
        response.setAssignmentTitle(assignmentTitle);
        response.setTotalQuestions(result.getTotalQuestions());
        response.setCorrectCount(result.getCorrectCount());
        response.setWrongCount(result.getWrongCount());
        response.setScore(result.getScore());
        response.setDurationSeconds(result.getDurationSeconds());
        response.setCompletedAt(result.getCompletedAt());

        List<AnswerDetailResponse> answerDetails = result.getAnswerDetails().stream()
                .map(detail -> {
                    AnswerDetailResponse adr = new AnswerDetailResponse();
                    adr.setQuestionId(detail.getQuestionId());
                    adr.setQuestionText(questionMap.containsKey(detail.getQuestionId())
                            ? questionMap.get(detail.getQuestionId()).getQuestion() : "Unknown");
                    adr.setSelectedAnswer(detail.getSelectedAnswer());
                    adr.setCorrectAnswer(detail.getCorrectAnswer());
                    adr.setIsCorrect(detail.getIsCorrect());
                    return adr;
                })
                .toList();

        response.setAnswerDetails(answerDetails);
        return response;
    }
}
