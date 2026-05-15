package com.example.webrtcbackend.lessons;

import com.example.webrtcbackend.courses.CourseService;
import com.example.webrtcbackend.courses.entity.Courses;
import com.example.webrtcbackend.lessons.entity.Lessons;
import com.example.webrtcbackend.user.UserRole;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LessonService {
    private final LessonRepository lessonRepository;
    private final CourseService courseService;

    public LessonService(LessonRepository lessonRepository, CourseService courseService) {
        this.lessonRepository = lessonRepository;
        this.courseService = courseService;
    }

    /**
     * Create a lesson. Verifies the user owns the parent course (or is ADMIN).
     */
    public Lessons createLesson(Long courseId, String title, String description,
                                Long currentUserId, UserRole currentUserRole) {
        Courses course = courseService.getCourseById(courseId);
        courseService.verifyOwnership(course, currentUserId, currentUserRole);

        Lessons lesson = new Lessons();
        lesson.setCourseId(courseId);
        lesson.setTitle(title);
        lesson.setDescription(description);
        lesson.setOrderIndex((int) (lessonRepository.count() + 1));
        return lessonRepository.save(lesson);
    }

    public Lessons getLessonById(Long id) {
        return lessonRepository.findById(id).orElseThrow(() -> new RuntimeException("Lesson not found with id: " + id));
    }

    /**
     * Update a lesson. Verifies ownership via parent course.
     */
    public Lessons updateLesson(Long id, String title, String content,
                                Long currentUserId, UserRole currentUserRole) {
        Lessons lesson = getLessonById(id);
        Courses course = courseService.getCourseById(lesson.getCourseId());
        courseService.verifyOwnership(course, currentUserId, currentUserRole);

        lesson.setTitle(title);
        lesson.setDescription(content);
        return lessonRepository.save(lesson);
    }

    /**
     * Delete a lesson. Verifies ownership via parent course.
     */
    public void deleteLesson(Long id, Long currentUserId, UserRole currentUserRole) {
        Lessons lesson = getLessonById(id);
        Courses course = courseService.getCourseById(lesson.getCourseId());
        courseService.verifyOwnership(course, currentUserId, currentUserRole);
        lessonRepository.delete(lesson);
    }

    public List<Lessons> searchLesson(String query) {
        if (query == null || query.trim().isEmpty()) {
            return lessonRepository.findAll();
        }
        return lessonRepository.findByTitleContainingIgnoreCase(query);
    }

    public List<Lessons> getLessonByCourseId(Long id) {
        return lessonRepository.findAll().stream()
                .filter(lesson -> lesson.getCourseId() != null && lesson.getCourseId().equals(id))
                .toList();
    }
}
