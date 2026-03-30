package com.example.webrtcbackend.transcription;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;


@Service
public class GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);

    @Value("${gemini.api-key}")
    private String apiKey;

    @Value("${gemini.url}")
    private String url;

    private final RestTemplate restTemplate = new RestTemplate();

    public String generateQuestions(String text) {

        String fullUrl = url + "?key=" + apiKey;

        String prompt = """
            You are an English teacher.

            Generate exactly 10 multiple-choice questionDTOS based ONLY on the text.

            STRICT RULES:
            1. Output must be VALID JSON
            2. Do not wrap JSON in markdown (no ```json)
            3. Do not add any explanation
            4. Do not add text before or after JSON
            5. Always include exactly 10 questionDTOS
            6. Each question must have 4 options (A, B, C, D)
            7. Only ONE correct answer per question

            JSON format:
            [
              {
                "question": "string",
                "options": {
                  "A": "string",
                  "B": "string",
                  "C": "string",
                  "D": "string"
                },
                "answer": "A"
              }
            ]

            Text:
            """ + text;

        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of(
                                "parts", List.of(
                                        Map.of("text", prompt)
                                )
                        )
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response =
                restTemplate.exchange(fullUrl, HttpMethod.POST, request, Map.class);

        String raw = extractText(response.getBody());

        if (raw != null) {
            raw = raw.replace("```json", "")
                    .replace("```", "")
                    .trim();
        }

        return raw;
    }

    private String extractText(Map response) {
        try {
            List candidates = (List) response.get("candidates");
            Map first = (Map) candidates.get(0);

            Map content = (Map) first.get("content");
            List parts = (List) content.get("parts");

            Map textPart = (Map) parts.get(0);

            return (String) textPart.get("text");
        } catch (Exception e) {
            log.error("Parse response error", e);
            return null;
        }
    }
}
