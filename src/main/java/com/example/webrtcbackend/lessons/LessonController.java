package com.example.webrtcbackend.lessons;

import com.example.webrtcbackend.lessons.dto.LessonDTO;
import com.example.webrtcbackend.lessons.entity.Lessons;
import com.example.webrtcbackend.video.VideoMetadata;
import com.example.webrtcbackend.video.VideoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lessons")
public class LessonController {
    private final LessonService lessonService;
    private final VideoService videoService;

    public LessonController(LessonService lessonService, VideoService videoService) {
        this.lessonService = lessonService;
        this.videoService = videoService;
    }

    @GetMapping("/search")
    public ResponseEntity<List<Lessons>> searchLessons(@RequestParam(value = "query", required = false) String query) {
        List<Lessons> results = lessonService.searchLesson(query);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Lessons> findById(@PathVariable(value = "id", required = true) Long id) {
        Lessons results = lessonService.getLessonById(id);
        return ResponseEntity.ok(results);
    }

    @PostMapping("/create")
    public ResponseEntity<Lessons> create(@RequestBody LessonDTO request) {
        Lessons results = lessonService.createLesson(request.getCourseId(), request.getTitle(), request.getDescription());
        return ResponseEntity.ok(results);
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<Lessons> update(@RequestBody LessonDTO request, @PathVariable Long id) {
        Lessons results = lessonService.updateLesson(id, request.getTitle(), request.getDescription());
        return ResponseEntity.ok(results);
    }

    @DeleteMapping("/delete/{id}")
    public void delete(@PathVariable Long id) {
        lessonService.deleteLesson(id);
    }

    @GetMapping("/get-by-course/{id}")
    public ResponseEntity<List<Lessons>> getLessonByCourseId(@PathVariable Long id) {
        return ResponseEntity.ok(lessonService.getLessonByCourseId(id));
    }

    @GetMapping("/get-videos/course/{courseId}/lesson/{lessonId}")
    public ResponseEntity<List<VideoMetadata>> getListVideo(@PathVariable Long lessonId, @PathVariable Long courseId) {
        return ResponseEntity.ok(videoService.getListVideo(courseId, lessonId));
    }

}
