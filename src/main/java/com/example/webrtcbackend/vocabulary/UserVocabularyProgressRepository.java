package com.example.webrtcbackend.vocabulary;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserVocabularyProgressRepository extends JpaRepository<UserVocabularyProgress, Long> {

    Optional<UserVocabularyProgress> findByUserIdAndVocabularyId(Long userId, Long vocabularyId);

    List<UserVocabularyProgress> findByUserId(Long userId);

    List<UserVocabularyProgress> findByUserIdAndLearnedFlagTrue(Long userId);

    List<UserVocabularyProgress> findByUserIdAndFavoriteFlagTrue(Long userId);

    List<UserVocabularyProgress> findByUserIdAndNeedReviewFlagTrue(Long userId);

    long countByUserIdAndLearnedFlagTrue(Long userId);

    long countByUserIdAndFavoriteFlagTrue(Long userId);

    long countByUserIdAndNeedReviewFlagTrue(Long userId);

    @Query("SELECT uvp.vocabularyId FROM UserVocabularyProgress uvp WHERE uvp.userId = :userId AND uvp.learnedFlag = true")
    List<Long> findLearnedVocabularyIdsByUserId(@Param("userId") Long userId);

    @Query("SELECT uvp.vocabularyId FROM UserVocabularyProgress uvp WHERE uvp.userId = :userId")
    List<Long> findVocabularyIdsByUserId(@Param("userId") Long userId);
}
