package com.example.webrtcbackend.transcription;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TranscriptRepository extends JpaRepository<Transcript, Long> {

    Optional<Transcript> findByAssemblyTranscriptId(String assemblyTranscriptId);

    List<Transcript> findByVideoId(Long videoId);
}
