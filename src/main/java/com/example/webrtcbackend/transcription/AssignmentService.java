package com.example.webrtcbackend.transcription;

import com.example.webrtcbackend.common.NotFoundException;
import com.example.webrtcbackend.transcription.dto.AssignmentResponse;
import com.example.webrtcbackend.transcription.dto.CreateAssignmentRequest;
import com.example.webrtcbackend.transcription.dto.QuestionDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AssignmentService {

    private static final Logger log = LoggerFactory.getLogger(AssignmentService.class);

    private final AssignmentRepository assignmentRepository;
    private final QuestionRepository questionRepository;
    private final OptionRepository optionRepository;
    private final RedisQueueService redisQueueService;

    public AssignmentService(AssignmentRepository assignmentRepository,
                             QuestionRepository questionRepository,
                             OptionRepository optionRepository,
                             RedisQueueService redisQueueService) {
        this.assignmentRepository = assignmentRepository;
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
        this.redisQueueService = redisQueueService;
    }

    /**
     * Create a new assignment with status PROCESSING and push to Redis queue.
     */
    @Transactional
    public AssignmentResponse createAssignment(CreateAssignmentRequest request) {
        log.info("Creating assignment for lessonId={}, title='{}'", request.getLessonId(), request.getTitle());

        Assignment assignment = new Assignment();
        assignment.setLessonId(request.getLessonId());
        assignment.setTitle(request.getTitle());
        assignment.setDescription(request.getDescription());
        assignment.setStatusProgress("PROCESSING");
        assignment.setRetryCount(0);
        assignment.setCreatedAt(Instant.now());

        assignment = assignmentRepository.save(assignment);
        log.info("Assignment created with id={}, status=PROCESSING", assignment.getId());

        // Push to Redis queue for async video processing
        redisQueueService.pushToAssignmentQueue(assignment.getId());

        return toResponse(assignment, Collections.emptyList());
    }

    /**
     * Get assignment by ID with questions and options.
     */
    @Transactional(readOnly = true)
    public AssignmentResponse getAssignmentById(Long id) {
        Assignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Assignment not found with id=" + id));

        List<Question> questions = questionRepository.findByAssignmentId(id);
        List<QuestionDTO> questionDTOs = buildQuestionDTOs(questions);

        return toResponse(assignment, questionDTOs);
    }

    /**
     * Get all assignments for a lesson with questions and options.
     */
    @Transactional(readOnly = true)
    public List<AssignmentResponse> getAssignmentsByLessonId(Long lessonId) {
        List<Assignment> assignments = assignmentRepository.findByLessonId(lessonId);

        return assignments.stream().map(assignment -> {
            List<Question> questions = questionRepository.findByAssignmentId(assignment.getId());
            List<QuestionDTO> questionDTOs = buildQuestionDTOs(questions);
            return toResponse(assignment, questionDTOs);
        }).toList();
    }

    /**
     * Build QuestionDTO list with options from DB entities.
     */
    private List<QuestionDTO> buildQuestionDTOs(List<Question> questions) {
        if (questions.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> questionIds = questions.stream()
                .map(Question::getId)
                .toList();

        List<Option> allOptions = optionRepository.findByQuestionIdIn(questionIds);

        // Group options by questionId
        Map<Long, List<Option>> optionsByQuestion = allOptions.stream()
                .collect(Collectors.groupingBy(Option::getQuestionId));

        return questions.stream().map(q -> {
            Map<String, String> optionsMap = new LinkedHashMap<>();
            List<Option> questionOptions = optionsByQuestion.getOrDefault(q.getId(), Collections.emptyList());
            for (Option opt : questionOptions) {
                optionsMap.put(opt.getOptionKey(), opt.getOptionValue());
            }
            return new QuestionDTO(q.getQuestion(), optionsMap, q.getCorrectAnswer());
        }).toList();
    }

    private AssignmentResponse toResponse(Assignment assignment, List<QuestionDTO> questions) {
        AssignmentResponse response = new AssignmentResponse();
        response.setId(assignment.getId());
        response.setLessonId(assignment.getLessonId());
        response.setTitle(assignment.getTitle());
        response.setDescription(assignment.getDescription());
        response.setStatusProgress(assignment.getStatusProgress());
        response.setErrorMessage(assignment.getErrorMessage());
        response.setCreatedAt(assignment.getCreatedAt());
        response.setQuestions(questions);
        return response;
    }
}
