package com.example.webrtcbackend.transcription;

import com.example.webrtcbackend.common.ApiResponse;
import com.example.webrtcbackend.transcription.dto.QuestionsResponse;
import com.example.webrtcbackend.transcription.dto.TranscribeRequest;
import com.example.webrtcbackend.transcription.dto.TranscribeResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transcriptions")
public class TranscriptionController {

    private static final Logger log = LoggerFactory.getLogger(TranscriptionController.class);

    private final TranscriptionService transcriptionService;

    private final GeminiService geminiService;

    public TranscriptionController(TranscriptionService transcriptionService, GeminiService geminiService) {
        this.transcriptionService = transcriptionService;
        this.geminiService = geminiService;
    }

    /**
     * POST /api/transcriptions/transcribe-from-url
     *
     * Accepts a video URL (Cloudflare R2 public URL) and returns the
     * full transcript text once transcription is complete.
     */
    @PostMapping("/transcribe-from-url")
    public ResponseEntity<ApiResponse<QuestionsResponse>> transcribeFromUrl(
            @Valid @RequestBody TranscribeRequest request) {
        log.info("POST /api/transcriptions/transcribe-from-url – url={}", request.getVideoUrl());
        QuestionsResponse response = transcriptionService.transcribe(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
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
            @PathVariable String videoId) {
        List<TranscribeResponse> responses = transcriptionService.getByVideoId(videoId);
        return ResponseEntity.ok(ApiResponse.ok(responses));
    }

    @PostMapping("/questions")
    public String generate(@RequestBody String text) {
        return geminiService.generateQuestions(text);
    }
}
