package com.example.webrtcbackend.transcription.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public class UpdateQuestionRequest {

    @NotBlank(message = "Question text is required")
    private String question;

    @NotBlank(message = "Correct answer is required")
    private String correctAnswer;

    @NotNull(message = "Options are required")
    private Map<String, String> options;

    public UpdateQuestionRequest() {}

    public UpdateQuestionRequest(String question, String correctAnswer, Map<String, String> options) {
        this.question = question;
        this.correctAnswer = correctAnswer;
        this.options = options;
    }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public String getCorrectAnswer() { return correctAnswer; }
    public void setCorrectAnswer(String correctAnswer) { this.correctAnswer = correctAnswer; }

    public Map<String, String> getOptions() { return options; }
    public void setOptions(Map<String, String> options) { this.options = options; }
}
