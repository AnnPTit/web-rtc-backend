package com.example.webrtcbackend.transcription.dto;

import java.util.List;


public class QuestionsResponse {
    private List<QuestionDTO> questionDTOS;

    public QuestionsResponse(List<QuestionDTO> questionDTOS) {
        this.questionDTOS = questionDTOS;
    }

    public List<QuestionDTO> getQuestions() {
        return questionDTOS;
    }

    public void setQuestions(List<QuestionDTO> questionDTOS) {
        this.questionDTOS = questionDTOS;
    }
}
