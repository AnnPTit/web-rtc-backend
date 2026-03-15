package com.example.webrtcbackend.video;

import com.example.webrtcbackend.video.dto.PresignRequest;
import com.example.webrtcbackend.video.dto.PresignResponse;
import com.example.webrtcbackend.video.dto.VideoMetadataDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/videos")
public class VideoController {

    private final VideoService videoService;

    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    /**
     * Generate a presigned URL that the frontend can use to PUT the video directly to R2.
     * The URL expires in a short time (10 minutes).
     */
    @PostMapping("/presign")
    public ResponseEntity<PresignResponse> presign(@Valid @RequestBody PresignRequest request) {
        PresignResponse resp = videoService.createPresignedUrl(request);
        return ResponseEntity.ok(resp);
    }

    /**
     * Save metadata after upload has completed.  The service will verify that the object
     * exists in R2 and record the playback URL along with other fields.
     */
    @PostMapping("/metadata")
    public ResponseEntity<VideoMetadata> saveMetadata(@Valid @RequestBody VideoMetadataDto dto) {
        VideoMetadata saved = videoService.saveMetadata(dto);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @GetMapping("/signed-url")
    public Map<String, String> getSignedUrl(@RequestParam String key) {

        String url = videoService.generateVideoUrl(key);

        return Map.of("signedUrl", url);
    }
}
