package com.example.webrtcbackend.lessons;

import com.example.webrtcbackend.auth.repository.AuthRepository;
import com.example.webrtcbackend.lessons.dto.LessonDTO;
import com.example.webrtcbackend.lessons.entity.Lessons;
import com.example.webrtcbackend.user.User;
import com.example.webrtcbackend.video.VideoMetadata;
import com.example.webrtcbackend.video.VideoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lessons")
public class LessonController {
    private final LessonService lessonService;
    private final VideoService videoService;
    private final AuthRepository authRepository;

    public LessonController(LessonService lessonService, VideoService videoService, AuthRepository authRepository) {
        this.lessonService = lessonService;
        this.videoService = videoService;
        this.authRepository = authRepository;
    }

    @GetMapping("/search")
    public ResponseEntity<List<Lessons>> searchLessons(@RequestParam(value = "query", required = false) String query) {
        List<Lessons> results = lessonService.searchLesson(query);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Lessons> findById(@PathVariable(value = "id") Long id) {
        Lessons results = lessonService.getLessonById(id);
        return ResponseEntity.ok(results);
    }

    @PostMapping("/create")
    public ResponseEntity<Lessons> create(@RequestBody LessonDTO request,
                                          @AuthenticationPrincipal UserDetails userDetails) {
        User user = resolveUser(userDetails);
        Lessons results = lessonService.createLesson(
                request.getCourseId(), request.getTitle(), request.getDescription(),
                user.getId(), user.getRole());
        return ResponseEntity.ok(results);
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<Lessons> update(@RequestBody LessonDTO request, @PathVariable Long id,
                                          @AuthenticationPrincipal UserDetails userDetails) {
        User user = resolveUser(userDetails);
        Lessons results = lessonService.updateLesson(
                id, request.getTitle(), request.getDescription(),
                user.getId(), user.getRole());
        return ResponseEntity.ok(results);
    }

    @DeleteMapping("/delete/{id}")
    public void delete(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        User user = resolveUser(userDetails);
        lessonService.deleteLesson(id, user.getId(), user.getRole());
    }

    @GetMapping("/get-by-course/{id}")
    public ResponseEntity<List<Lessons>> getLessonByCourseId(@PathVariable Long id) {
        return ResponseEntity.ok(lessonService.getLessonByCourseId(id));
    }

    @GetMapping("/get-videos/course/{courseId}/lesson/{lessonId}")
    public ResponseEntity<List<VideoMetadata>> getListVideo(@PathVariable Long lessonId, @PathVariable Long courseId) {
        return ResponseEntity.ok(videoService.getListVideo(courseId, lessonId));
    }

    private User resolveUser(UserDetails userDetails) {
        return authRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
