package com.example.webrtcbackend.vocabulary;

import com.example.webrtcbackend.common.NotFoundException;
import com.example.webrtcbackend.transcription.GeminiService;
import com.example.webrtcbackend.user.UserRepository;
import com.example.webrtcbackend.vocabulary.dto.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class VocabularyService {

    private static final Logger log = LoggerFactory.getLogger(VocabularyService.class);

    private final VocabularyWordRepository vocabularyWordRepository;
    private final UserVocabularyProgressRepository progressRepository;
    private final UserRepository userRepository;
    private final GeminiService geminiService;
    private final ObjectMapper objectMapper;

    public VocabularyService(VocabularyWordRepository vocabularyWordRepository,
                             UserVocabularyProgressRepository progressRepository,
                             UserRepository userRepository,
                             GeminiService geminiService,
                             ObjectMapper objectMapper) {
        this.vocabularyWordRepository = vocabularyWordRepository;
        this.progressRepository = progressRepository;
        this.userRepository = userRepository;
        this.geminiService = geminiService;
        this.objectMapper = objectMapper;
    }

    // ========================================================================
    // 1. Generate vocabulary (DB-first, then AI fallback)
    // ========================================================================

    @Transactional
    public List<VocabularyWordResponse> generateVocabulary(GenerateVocabularyRequest request) {
        log.info("Generating vocabulary: topic={}, level={}, quantity={}, userId={}",
                request.getTopic(), request.getLevel(), request.getQuantity(), request.getUserId());

        // Validate user exists
        userRepository.findById(request.getUserId())
                .orElseThrow(() -> new NotFoundException("User not found with id=" + request.getUserId()));

        // 1. Get IDs of words user has already learned
        List<Long> learnedIds = progressRepository.findLearnedVocabularyIdsByUserId(request.getUserId());
        log.info("User {} has learned {} words", request.getUserId(), learnedIds.size());

        // 2. Find existing unused words from DB
        List<VocabularyWord> availableWords;
        if (learnedIds.isEmpty()) {
            availableWords = vocabularyWordRepository.findByTopicAndLevel(
                    request.getTopic(), request.getLevel());
        } else {
            availableWords = vocabularyWordRepository.findByTopicAndLevelExcludingIds(
                    request.getTopic(), request.getLevel(), learnedIds);
        }
        log.info("Found {} available words in DB for topic={}, level={}",
                availableWords.size(), request.getTopic(), request.getLevel());

        List<VocabularyWord> resultWords = new ArrayList<>();

        // 3. If enough words in DB, take from there
        if (availableWords.size() >= request.getQuantity()) {
            // Shuffle and pick the requested quantity
            Collections.shuffle(availableWords);
            resultWords = availableWords.subList(0, request.getQuantity());
            log.info("Returning {} words from DB (no AI call needed)", resultWords.size());
        } else {
            // Add all available DB words
            resultWords.addAll(availableWords);
            int remaining = request.getQuantity() - resultWords.size();

            // 4. Collect existing words to exclude from AI generation
            List<String> excludeWords = new ArrayList<>();
            // Exclude already-learned words
            if (!learnedIds.isEmpty()) {
                vocabularyWordRepository.findAllById(learnedIds)
                        .forEach(w -> excludeWords.add(w.getWord()));
            }
            // Exclude available words we already picked
            resultWords.forEach(w -> excludeWords.add(w.getWord()));

            // 5. Call Gemini AI to generate more
            log.info("Need {} more words from AI, excluding {} words", remaining, excludeWords.size());
            List<VocabularyWord> aiWords = generateFromAI(
                    request.getTopic(), request.getLevel(), remaining, excludeWords);

            resultWords.addAll(aiWords);
            log.info("Total words after AI generation: {}", resultWords.size());
        }

        // 6. Build response with user progress
        Map<Long, UserVocabularyProgress> progressMap = loadProgressMap(request.getUserId(), resultWords);

        return resultWords.stream()
                .map(word -> toResponse(word, progressMap.get(word.getId())))
                .toList();
    }

    // ========================================================================
    // 2. Update learning progress
    // ========================================================================

    @Transactional
    public VocabularyWordResponse updateProgress(UpdateProgressRequest request) {
        log.info("Updating progress: userId={}, vocabularyId={}", request.getUserId(), request.getVocabularyId());

        // Validate vocabulary word exists
        VocabularyWord word = vocabularyWordRepository.findById(request.getVocabularyId())
                .orElseThrow(() -> new NotFoundException("Vocabulary word not found with id=" + request.getVocabularyId()));

        // Upsert progress
        UserVocabularyProgress progress = progressRepository
                .findByUserIdAndVocabularyId(request.getUserId(), request.getVocabularyId())
                .orElseGet(() -> {
                    UserVocabularyProgress p = new UserVocabularyProgress();
                    p.setUserId(request.getUserId());
                    p.setVocabularyId(request.getVocabularyId());
                    return p;
                });

        if (request.getLearnedFlag() != null) {
            progress.setLearnedFlag(request.getLearnedFlag());
            if (request.getLearnedFlag() && progress.getLearnedAt() == null) {
                progress.setLearnedAt(Instant.now());
            }
        }

        if (request.getFavoriteFlag() != null) {
            progress.setFavoriteFlag(request.getFavoriteFlag());
        }

        if (request.getNeedReviewFlag() != null) {
            progress.setNeedReviewFlag(request.getNeedReviewFlag());
        }

        progress = progressRepository.save(progress);
        log.info("Progress saved: id={}", progress.getId());

        return toResponse(word, progress);
    }

    // ========================================================================
    // 3. Get user progress
    // ========================================================================

    @Transactional(readOnly = true)
    public List<VocabularyWordResponse> getUserProgress(Long userId) {
        List<UserVocabularyProgress> progressList = progressRepository.findByUserId(userId);

        if (progressList.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> vocabIds = progressList.stream()
                .map(UserVocabularyProgress::getVocabularyId)
                .toList();

        Map<Long, VocabularyWord> wordMap = vocabularyWordRepository.findAllById(vocabIds).stream()
                .collect(Collectors.toMap(VocabularyWord::getId, Function.identity()));

        return progressList.stream()
                .filter(p -> wordMap.containsKey(p.getVocabularyId()))
                .map(p -> toResponse(wordMap.get(p.getVocabularyId()), p))
                .toList();
    }

    // ========================================================================
    // 4. Get user vocabulary stats
    // ========================================================================

    @Transactional(readOnly = true)
    public VocabularyStatsResponse getUserStats(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with id=" + userId));

        long totalLearned = progressRepository.countByUserIdAndLearnedFlagTrue(userId);
        long totalFavorites = progressRepository.countByUserIdAndFavoriteFlagTrue(userId);
        long totalNeedReview = progressRepository.countByUserIdAndNeedReviewFlagTrue(userId);
        long totalStudied = progressRepository.findByUserId(userId).size();

        // Topic breakdown of learned words
        List<UserVocabularyProgress> learnedProgress = progressRepository.findByUserIdAndLearnedFlagTrue(userId);
        Map<String, Long> topicBreakdown = new HashMap<>();

        if (!learnedProgress.isEmpty()) {
            List<Long> learnedVocabIds = learnedProgress.stream()
                    .map(UserVocabularyProgress::getVocabularyId)
                    .toList();
            List<VocabularyWord> learnedWords = vocabularyWordRepository.findAllById(learnedVocabIds);
            topicBreakdown = learnedWords.stream()
                    .collect(Collectors.groupingBy(VocabularyWord::getTopic, Collectors.counting()));
        }

        VocabularyStatsResponse stats = new VocabularyStatsResponse();
        stats.setUserId(userId);
        stats.setTotalWordsLearned(totalLearned);
        stats.setTotalFavorites(totalFavorites);
        stats.setTotalNeedReview(totalNeedReview);
        stats.setTotalWordsStudied(totalStudied);
        stats.setTopicBreakdown(topicBreakdown);
        return stats;
    }

    // ========================================================================
    // 5. Get vocabulary by topic with user progress
    // ========================================================================

    @Transactional(readOnly = true)
    public List<VocabularyWordResponse> getVocabularyByTopic(String topic, String level, Long userId) {
        List<VocabularyWord> words = vocabularyWordRepository.findByTopicAndLevel(topic, level);

        if (words.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, UserVocabularyProgress> progressMap = userId != null
                ? loadProgressMap(userId, words)
                : Collections.emptyMap();

        return words.stream()
                .map(w -> toResponse(w, progressMap.get(w.getId())))
                .toList();
    }

    // ========================================================================
    // 6. Get favorite words
    // ========================================================================

    @Transactional(readOnly = true)
    public List<VocabularyWordResponse> getFavoriteWords(Long userId) {
        List<UserVocabularyProgress> favorites = progressRepository.findByUserIdAndFavoriteFlagTrue(userId);

        if (favorites.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> vocabIds = favorites.stream()
                .map(UserVocabularyProgress::getVocabularyId)
                .toList();

        Map<Long, VocabularyWord> wordMap = vocabularyWordRepository.findAllById(vocabIds).stream()
                .collect(Collectors.toMap(VocabularyWord::getId, Function.identity()));

        return favorites.stream()
                .filter(p -> wordMap.containsKey(p.getVocabularyId()))
                .map(p -> toResponse(wordMap.get(p.getVocabularyId()), p))
                .toList();
    }

    // ========================================================================
    // 7. Get words needing review
    // ========================================================================

    @Transactional(readOnly = true)
    public List<VocabularyWordResponse> getReviewWords(Long userId) {
        List<UserVocabularyProgress> reviewList = progressRepository.findByUserIdAndNeedReviewFlagTrue(userId);

        if (reviewList.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> vocabIds = reviewList.stream()
                .map(UserVocabularyProgress::getVocabularyId)
                .toList();

        Map<Long, VocabularyWord> wordMap = vocabularyWordRepository.findAllById(vocabIds).stream()
                .collect(Collectors.toMap(VocabularyWord::getId, Function.identity()));

        return reviewList.stream()
                .filter(p -> wordMap.containsKey(p.getVocabularyId()))
                .map(p -> toResponse(wordMap.get(p.getVocabularyId()), p))
                .toList();
    }

    // ========================================================================
    // 8. Get available topics
    // ========================================================================

    public List<String> getAvailableTopics() {
        return List.of(
                "Travel",
                "Business",
                "Technology",
                "Daily Life",
                "Food",
                "Education",
                "Health",
                "Finance",
                "Office",
                "IELTS Speaking"
        );
    }

    // ========================================================================
    // Helper methods
    // ========================================================================

    private List<VocabularyWord> generateFromAI(String topic, String level, int quantity, List<String> excludeWords) {
        try {
            String rawJson = geminiService.generateVocabulary(topic, level, quantity, excludeWords);
            if (rawJson == null || rawJson.isBlank()) {
                log.error("Gemini returned empty response for vocabulary generation");
                return Collections.emptyList();
            }

            List<Map<String, String>> vocabItems = objectMapper.readValue(rawJson,
                    new TypeReference<List<Map<String, String>>>() {});

            List<VocabularyWord> newWords = new ArrayList<>();
            for (Map<String, String> item : vocabItems) {
                String word = item.get("word");
                if (word == null || word.isBlank()) continue;

                // Check for duplicates in DB
                Optional<VocabularyWord> existing = vocabularyWordRepository
                        .findByWordAndTopicAndLevel(word.toLowerCase().trim(), topic, level);
                if (existing.isPresent()) {
                    log.debug("Word '{}' already exists in DB, skipping", word);
                    continue;
                }

                VocabularyWord vocab = new VocabularyWord();
                vocab.setWord(word.toLowerCase().trim());
                vocab.setIpa(item.getOrDefault("ipa", ""));
                vocab.setWordType(item.getOrDefault("type", ""));
                vocab.setMeaningVi(item.getOrDefault("meaning_vi", ""));
                vocab.setMeaningEn(item.getOrDefault("meaning_en", ""));
                vocab.setExampleSentence(item.getOrDefault("example", ""));
                vocab.setExampleVi(item.getOrDefault("example_vi", ""));
                vocab.setTopic(topic);
                vocab.setLevel(level);
                vocab.setSourceAi("gemini");

                newWords.add(vocab);
            }

            // Save all new words
            if (!newWords.isEmpty()) {
                newWords = vocabularyWordRepository.saveAll(newWords);
                log.info("Saved {} new vocabulary words from AI", newWords.size());
            }

            return newWords;
        } catch (Exception e) {
            log.error("Failed to parse AI vocabulary response", e);
            return Collections.emptyList();
        }
    }

    private Map<Long, UserVocabularyProgress> loadProgressMap(Long userId, List<VocabularyWord> words) {
        List<Long> vocabIds = words.stream().map(VocabularyWord::getId).toList();
        return progressRepository.findByUserId(userId).stream()
                .filter(p -> vocabIds.contains(p.getVocabularyId()))
                .collect(Collectors.toMap(UserVocabularyProgress::getVocabularyId, Function.identity()));
    }

    private VocabularyWordResponse toResponse(VocabularyWord word, UserVocabularyProgress progress) {
        VocabularyWordResponse response = new VocabularyWordResponse();
        response.setId(word.getId());
        response.setWord(word.getWord());
        response.setIpa(word.getIpa());
        response.setWordType(word.getWordType());
        response.setMeaningVi(word.getMeaningVi());
        response.setMeaningEn(word.getMeaningEn());
        response.setExampleSentence(word.getExampleSentence());
        response.setExampleVi(word.getExampleVi());
        response.setTopic(word.getTopic());
        response.setLevel(word.getLevel());
        response.setCreatedAt(word.getCreatedAt());

        if (progress != null) {
            response.setLearned(progress.getLearnedFlag());
            response.setFavorite(progress.getFavoriteFlag());
            response.setNeedReview(progress.getNeedReviewFlag());
            response.setReviewCount(progress.getReviewCount());
            response.setCorrectCount(progress.getCorrectCount());
            response.setWrongCount(progress.getWrongCount());
        } else {
            response.setLearned(false);
            response.setFavorite(false);
            response.setNeedReview(false);
            response.setReviewCount(0);
            response.setCorrectCount(0);
            response.setWrongCount(0);
        }

        return response;
    }
}
