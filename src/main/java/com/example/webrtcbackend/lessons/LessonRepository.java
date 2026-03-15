package com.example.webrtcbackend.lessons;

import com.example.webrtcbackend.lessons.entity.Lessons;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LessonRepository extends JpaRepository<Lessons, Long> {
    List<Lessons> findByTitleContainingIgnoreCase(String query);
}
