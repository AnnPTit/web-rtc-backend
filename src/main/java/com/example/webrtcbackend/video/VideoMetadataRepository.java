package com.example.webrtcbackend.video;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VideoMetadataRepository extends JpaRepository<VideoMetadata, Long> {
    List<VideoMetadata> findByCourseIdAndLessonId(Long courseId, Long lessonId);
    // additional query methods can be added here
}
