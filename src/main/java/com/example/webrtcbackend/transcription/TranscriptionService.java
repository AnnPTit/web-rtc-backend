package com.example.webrtcbackend.transcription;

import com.example.webrtcbackend.transcription.dto.TranscribeRequest;
import com.example.webrtcbackend.transcription.dto.TranscribeResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class TranscriptionService {

    private static final Logger log = LoggerFactory.getLogger(TranscriptionService.class);

    /**
     * AssemblyAI API base URL.
     */
    private static final String ASSEMBLYAI_BASE_URL = "https://api.assemblyai.com";

    /**
     * How often (ms) we poll AssemblyAI for status updates.
     */
    private static final long POLL_INTERVAL_MS = 5_000;

    /**
     * Maximum time (ms) we will wait for transcription to complete before giving up.
     */
    private static final long MAX_WAIT_MS = 10 * 60 * 1_000; // 10 minutes

    /**
     * Maximum number of HTTP retries for transient errors.
     */
    private static final int MAX_RETRIES = 3;

    private final RestTemplate restTemplate;
    private final TranscriptRepository transcriptRepository;
    private final String apiKey;

    public TranscriptionService(RestTemplate restTemplate,
                                TranscriptRepository transcriptRepository,
                                @Value("${assemblyai.api-key}") String apiKey) {
        this.restTemplate = restTemplate;
        this.transcriptRepository = transcriptRepository;
        this.apiKey = apiKey;
    }

    // -----------------------------------------------------------------------
    //  Public API
    // -----------------------------------------------------------------------

    /**
     * Submit a video URL for transcription, poll until complete, persist the
     * result and return a {@link TranscribeResponse}.
     */
    public TranscribeResponse transcribe(TranscribeRequest request) {
        log.info("Starting transcription for URL: {}", request.getVideoUrl());

        // 1. Submit transcription request to AssemblyAI
        String transcriptId = submitTranscription(request.getVideoUrl());
        log.info("AssemblyAI accepted transcription – id={}", transcriptId);

        // 2. Create a DB record in "processing" state
        Transcript entity = new Transcript();
        entity.setAssemblyTranscriptId(transcriptId);
        entity.setVideoUrl(request.getVideoUrl());
        entity.setVideoId(request.getVideoId());
        entity.setStatus("processing");
        entity.setCreatedAt(Instant.now());
        entity = transcriptRepository.save(entity);
        log.debug("Saved initial transcript entity id={}", entity.getId());

        // 3. Poll for result
        Map<String, Object> result = pollForCompletion(transcriptId);

        // 4. Update entity with the final result
        String status = (String) result.get("status");
        entity.setStatus(status);

        if ("completed".equals(status)) {
            String text = (String) result.get("text");
            entity.setTranscriptText(text);
            entity.setCompletedAt(Instant.now());
            log.info("Transcription completed – id={}, length={}", transcriptId,
                    text != null ? text.length() : 0);
        } else {
            // error
            String error = (String) result.get("error");
            entity.setErrorMessage(error);
            log.error("Transcription failed – id={}, error={}", transcriptId, error);
        }

        entity = transcriptRepository.save(entity);

        return toResponse(entity);
    }

    /**
     * Retrieve a previously saved transcript by its database ID.
     */
    public TranscribeResponse getById(Long id) {
        Transcript entity = transcriptRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Transcript not found with id=" + id));
        return toResponse(entity);
    }

    /**
     * Retrieve all transcripts linked to a specific video.
     */
    public List<TranscribeResponse> getByVideoId(Long videoId) {
        return transcriptRepository.findByVideoId(videoId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // -----------------------------------------------------------------------
    //  AssemblyAI integration (private)
    // -----------------------------------------------------------------------

    /**
     * POST /v2/transcript – submits the audio/video URL for transcription.
     *
     * @return the AssemblyAI transcript ID
     */
    private String submitTranscription(String audioUrl) {
        String url = ASSEMBLYAI_BASE_URL + "/v2/transcript";

        Map<String, Object> body = Map.of(
                "audio_url", audioUrl,
                "speech_models", List.of("universal-3-pro", "universal-2"),
                "language_detection", true
        );

        HttpHeaders headers = buildHeaders();
        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = executeWithRetry(() ->
                restTemplate.exchange(url, HttpMethod.POST, httpEntity, Map.class)
        );

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "AssemblyAI returned unexpected response: " + response.getStatusCode());
        }

        String transcriptId = (String) response.getBody().get("id");
        if (transcriptId == null || transcriptId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "AssemblyAI did not return a transcript ID");
        }

        return transcriptId;
    }

    /**
     * Poll GET /v2/transcript/{id} until the status is "completed" or "error",
     * or until the timeout is exceeded.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> pollForCompletion(String transcriptId) {
        String url = ASSEMBLYAI_BASE_URL + "/v2/transcript/" + transcriptId;

        HttpHeaders headers = buildHeaders();
        HttpEntity<Void> httpEntity = new HttpEntity<>(headers);

        long startTime = System.currentTimeMillis();

        while (true) {
            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed >= MAX_WAIT_MS) {
                log.error("Transcription polling timed out after {}ms – id={}", elapsed, transcriptId);
                throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT,
                        "Transcription timed out after " + (MAX_WAIT_MS / 1000) + " seconds");
            }

            ResponseEntity<Map> response = executeWithRetry(() ->
                    restTemplate.exchange(url, HttpMethod.GET, httpEntity, Map.class)
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Failed to poll transcription status");
            }

            Map<String, Object> body = response.getBody();
            String status = (String) body.get("status");
            log.debug("Poll transcription id={} – status={}", transcriptId, status);

            if ("completed".equals(status) || "error".equals(status)) {
                return body;
            }

            // status is "queued" or "processing" – wait before next poll
            try {
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Polling interrupted");
            }
        }
    }

    // -----------------------------------------------------------------------
    //  Helpers
    // -----------------------------------------------------------------------

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("authorization", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    /**
     * Simple retry wrapper for transient HTTP errors (5xx, connection errors).
     */
    @SuppressWarnings("unchecked")
    private <T> T executeWithRetry(java.util.function.Supplier<T> action) {
        int attempt = 0;
        while (true) {
            try {
                return action.get();
            } catch (RestClientException ex) {
                attempt++;
                if (attempt >= MAX_RETRIES) {
                    log.error("HTTP request failed after {} retries: {}", MAX_RETRIES, ex.getMessage());
                    throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                            "Failed to communicate with AssemblyAI after " + MAX_RETRIES + " retries", ex);
                }
                log.warn("HTTP request failed (attempt {}/{}), retrying in 2s: {}",
                        attempt, MAX_RETRIES, ex.getMessage());
                try {
                    Thread.sleep(2_000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Retry interrupted");
                }
            }
        }
    }

    private TranscribeResponse toResponse(Transcript entity) {
        TranscribeResponse resp = new TranscribeResponse();
        resp.setId(entity.getId());
        resp.setTranscriptId(entity.getAssemblyTranscriptId());
        resp.setVideoUrl(entity.getVideoUrl());
        resp.setStatus(entity.getStatus());
        resp.setTranscriptText(entity.getTranscriptText());
        resp.setErrorMessage(entity.getErrorMessage());
        resp.setCreatedAt(entity.getCreatedAt());
        resp.setCompletedAt(entity.getCompletedAt());
        return resp;
    }
}
