package com.example.webrtcbackend.lessons;

import com.example.webrtcbackend.courses.entity.Courses;
import com.example.webrtcbackend.lessons.entity.Lessons;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LessonService {
    private final LessonRepository lessonRepository;

    public LessonService(LessonRepository lessonRepository) {
        this.lessonRepository = lessonRepository;
    }

    public Lessons createLesson(Long courseId, String title, String description) {
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

    public Lessons updateLesson(Long id, String title, String content) {
        Lessons lesson = getLessonById(id);
        lesson.setTitle(title);
        lesson.setDescription(content);
        return lessonRepository.save(lesson);
    }

    public void deleteLesson(Long id) {
        Lessons lesson = getLessonById(id);
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
