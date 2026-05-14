package com.example.webrtcbackend.courses;

import com.example.webrtcbackend.courses.entity.CourseLevel;
import com.example.webrtcbackend.courses.entity.Courses;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CourseService {
    private final CoursesRepository coursesRepository;

    public CourseService(CoursesRepository coursesRepository) {
        this.coursesRepository = coursesRepository;
    }

    public Courses createCourse(String title, String description, CourseLevel level) {
        Courses course = new Courses();
        course.setTitle(title);
        course.setDescription(description);
        course.setLevel(level != null ? level : CourseLevel.BEGINNER);
        return coursesRepository.save(course);
    }

    public Courses getCourseById(Long id) {
        return coursesRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found with id: " + id));
    }

    public Courses updateCourse(Long id, String title, String description, CourseLevel level) {
        Courses course = getCourseById(id);
        course.setTitle(title);
        course.setDescription(description);
        if (level != null) {
            course.setLevel(level);
        }
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
