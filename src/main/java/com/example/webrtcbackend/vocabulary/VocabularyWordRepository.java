package com.example.webrtcbackend.vocabulary;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VocabularyWordRepository extends JpaRepository<VocabularyWord, Long> {

    List<VocabularyWord> findByTopicAndLevel(String topic, String level);

    Optional<VocabularyWord> findByWordAndTopicAndLevel(String word, String topic, String level);

    long countByTopicAndLevel(String topic, String level);

    @Query("SELECT v FROM VocabularyWord v WHERE v.topic = :topic AND v.level = :level AND v.id NOT IN :excludeIds")
    List<VocabularyWord> findByTopicAndLevelExcludingIds(
            @Param("topic") String topic,
            @Param("level") String level,
            @Param("excludeIds") List<Long> excludeIds);
}
