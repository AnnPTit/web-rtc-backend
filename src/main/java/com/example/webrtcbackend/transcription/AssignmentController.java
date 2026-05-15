package com.example.webrtcbackend.transcription;

import com.example.webrtcbackend.common.ApiResponse;
import com.example.webrtcbackend.transcription.dto.AssignmentResponse;
import com.example.webrtcbackend.transcription.dto.CreateAssignmentRequest;
import com.example.webrtcbackend.transcription.dto.QuestionDTO;
import com.example.webrtcbackend.transcription.dto.UpdateQuestionRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assignments")
public class AssignmentController {

    private static final Logger log = LoggerFactory.getLogger(AssignmentController.class);

    private final AssignmentService assignmentService;

    public AssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    /**
     * POST /api/assignments
     * Create a new assignment — synchronously transcribes video and generates questions.
     * This is a blocking call that may take 1-5 minutes.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<AssignmentResponse>> createAssignment(
            @Valid @RequestBody CreateAssignmentRequest request) {
        log.info("POST /api/assignments – lessonId={}, title='{}'", request.getLessonId(), request.getTitle());
        AssignmentResponse response = assignmentService.createAssignment(request);
        return ResponseEntity.ok(ApiResponse.ok("Assignment created", response));
    }

    /**
     * GET /api/assignments/{id}
     * Retrieve assignment with questions and options.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AssignmentResponse>> getAssignment(@PathVariable Long id) {
        log.info("GET /api/assignments/{}", id);
        AssignmentResponse response = assignmentService.getAssignmentById(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * GET /api/assignments/by-lesson/{lessonId}
     * Retrieve all assignments for a lesson.
     */
    @GetMapping("/by-lesson/{lessonId}")
    public ResponseEntity<ApiResponse<List<AssignmentResponse>>> getAssignmentsByLesson(
            @PathVariable Long lessonId) {
        log.info("GET /api/assignments/by-lesson/{}", lessonId);
        List<AssignmentResponse> responses = assignmentService.getAssignmentsByLessonId(lessonId);
        return ResponseEntity.ok(ApiResponse.ok(responses));
    }

    /**
     * PUT /api/assignments/{assignmentId}/questions/{questionId}
     * Update a question's text, correct answer, and options.
     */
    @PutMapping("/{assignmentId}/questions/{questionId}")
    public ResponseEntity<ApiResponse<QuestionDTO>> updateQuestion(
            @PathVariable Long assignmentId,
            @PathVariable Long questionId,
            @Valid @RequestBody UpdateQuestionRequest request) {
        log.info("PUT /api/assignments/{}/questions/{}", assignmentId, questionId);
        QuestionDTO updated = assignmentService.updateQuestion(assignmentId, questionId, request);
        return ResponseEntity.ok(ApiResponse.ok("Question updated", updated));
    }

    /**
     * DELETE /api/assignments/{assignmentId}/questions/{questionId}
     * Delete a question and its options.
     */
    @DeleteMapping("/{assignmentId}/questions/{questionId}")
    public ResponseEntity<ApiResponse<Void>> deleteQuestion(
            @PathVariable Long assignmentId,
            @PathVariable Long questionId) {
        log.info("DELETE /api/assignments/{}/questions/{}", assignmentId, questionId);
        assignmentService.deleteQuestion(assignmentId, questionId);
        return ResponseEntity.ok(ApiResponse.ok("Question deleted", null));
    }
}
