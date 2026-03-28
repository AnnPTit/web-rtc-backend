package com.example.webrtcbackend.transcription;

import com.example.webrtcbackend.common.ApiResponse;
import com.example.webrtcbackend.transcription.dto.TranscribeRequest;
import com.example.webrtcbackend.transcription.dto.TranscribeResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transcriptions")
public class TranscriptionController {

    private static final Logger log = LoggerFactory.getLogger(TranscriptionController.class);

    private final TranscriptionService transcriptionService;

    public TranscriptionController(TranscriptionService transcriptionService) {
        this.transcriptionService = transcriptionService;
    }

    /**
     * POST /api/transcriptions/transcribe-from-url
     *
     * Accepts a video URL (Cloudflare R2 public URL) and returns the
     * full transcript text once transcription is complete.
     */
    @PostMapping("/transcribe-from-url")
    public ResponseEntity<ApiResponse<TranscribeResponse>> transcribeFromUrl(
            @Valid @RequestBody TranscribeRequest request) {

        log.info("POST /api/transcriptions/transcribe-from-url – url={}", request.getVideoUrl());

        TranscribeResponse response = transcriptionService.transcribe(request);

        if ("error".equals(response.getStatus())) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(ApiResponse.error("Transcription failed: " + response.getErrorMessage()));
        }

        return ResponseEntity.ok(ApiResponse.ok("Transcription completed", response));
    }

    /**
     * GET /api/transcriptions/{id}
     *
     * Retrieve a stored transcript by its database ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TranscribeResponse>> getById(@PathVariable Long id) {
        TranscribeResponse response = transcriptionService.getById(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * GET /api/transcriptions/by-video/{videoId}
     *
     * Retrieve all transcripts linked to a specific video.
     */
    @GetMapping("/by-video/{videoId}")
    public ResponseEntity<ApiResponse<List<TranscribeResponse>>> getByVideoId(
            @PathVariable Long videoId) {
        List<TranscribeResponse> responses = transcriptionService.getByVideoId(videoId);
        return ResponseEntity.ok(ApiResponse.ok(responses));
    }
}
