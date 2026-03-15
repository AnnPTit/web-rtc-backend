package com.example.webrtcbackend.courses;

import com.example.webrtcbackend.courses.entity.Courses;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CoursesRepository extends JpaRepository<Courses, Long> {
    List<Courses> findByTitleContainingIgnoreCase(String query);
}
