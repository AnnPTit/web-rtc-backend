package com.example.webrtcbackend.video;

import com.example.webrtcbackend.auth.repository.AuthRepository;
import com.example.webrtcbackend.courses.CourseService;
import com.example.webrtcbackend.courses.entity.Courses;
import com.example.webrtcbackend.user.User;
import com.example.webrtcbackend.video.dto.PresignRequest;
import com.example.webrtcbackend.video.dto.PresignResponse;
import com.example.webrtcbackend.video.dto.VideoMetadataDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/videos")
public class VideoController {

    private final VideoService videoService;
    private final CourseService courseService;
    private final AuthRepository authRepository;

    public VideoController(VideoService videoService, CourseService courseService, AuthRepository authRepository) {
        this.videoService = videoService;
        this.courseService = courseService;
        this.authRepository = authRepository;
    }

    /**
     * Generate a presigned URL that the frontend can use to PUT the video directly to R2.
     * Verifies the user owns the course before generating the URL.
     */
    @PostMapping("/presign")
    public ResponseEntity<PresignResponse> presign(@Valid @RequestBody PresignRequest request,
                                                   @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails != null) {
            User user = resolveUser(userDetails);
            Courses course = courseService.getCourseById(request.getCourseId());
            courseService.verifyOwnership(course, user.getId(), user.getRole());
        }
        PresignResponse resp = videoService.createPresignedUrl(request);
        return ResponseEntity.ok(resp);
    }

    /**
     * Save metadata after upload has completed. Verifies ownership.
     */
    @PostMapping("/metadata")
    public ResponseEntity<VideoMetadata> saveMetadata(@Valid @RequestBody VideoMetadataDto dto,
                                                      @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails != null) {
            User user = resolveUser(userDetails);
            Courses course = courseService.getCourseById(dto.getCourseId());
            courseService.verifyOwnership(course, user.getId(), user.getRole());
        }
        VideoMetadata saved = videoService.saveMetadata(dto);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @GetMapping("/signed-url")
    public Map<String, String> getSignedUrl(@RequestParam String key) {
        String url = videoService.generateVideoUrl(key);
        return Map.of("signedUrl", url);
    }

    private User resolveUser(UserDetails userDetails) {
        return authRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
