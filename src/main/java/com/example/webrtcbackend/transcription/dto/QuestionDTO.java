package com.example.webrtcbackend.transcription.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class QuestionDTO {
    private String question;
    private Map<String, String> options;
    private String answer;

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public Map<String, String> getOptions() {
        return options;
    }

    public void setOptions(Map<String, String> options) {
        this.options = options;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public QuestionDTO(String question, Map<String, String> options, String answer) {
        this.question = question;
        this.options = options;
        this.answer = answer;
    }

    public QuestionDTO() {
    }
}
