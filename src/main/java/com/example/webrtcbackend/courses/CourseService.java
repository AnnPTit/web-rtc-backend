package com.example.webrtcbackend.courses;

import com.example.webrtcbackend.courses.entity.Courses;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CourseService {
    private final CoursesRepository coursesRepository;

    public CourseService(CoursesRepository coursesRepository) {
        this.coursesRepository = coursesRepository;
    }

    public Courses createCourse(String title, String description) {
        Courses course = new Courses();
        course.setTitle(title);
        course.setDescription(description);
        return coursesRepository.save(course);
    }

    public Courses getCourseById(Long id) {
        return coursesRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found with id: " + id));
    }

    public Courses updateCourse(Long id, String title, String description) {
        Courses course = getCourseById(id);
        course.setTitle(title);
        course.setDescription(description);
        return coursesRepository.save(course);
    }

    public void deleteCourse(Long id) {
        Courses course = getCourseById(id);
        coursesRepository.delete(course);
    }

    public List<Courses> searchCourses(String query) {
        if (query == null || query.trim().isEmpty()) {
            return coursesRepository.findAll();
        }
        return coursesRepository.findByTitleContainingIgnoreCase(query);
    }


}
