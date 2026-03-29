package com.example.webrtcbackend.transcription;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OptionRepository extends JpaRepository<Option, Long> {

    List<Option> findByQuestionId(Long questionId);

    List<Option> findByQuestionIdIn(List<Long> questionIds);
}
