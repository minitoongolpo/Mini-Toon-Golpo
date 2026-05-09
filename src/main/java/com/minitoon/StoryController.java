package com.minitoon;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

@RestController
public class StoryController {

    @Value("${GEMINI_API_KEY:}")
    private String geminiApiKey;

    @GetMapping("/generate-story")
    public String generateStory() {

        try {

            String prompt = "Write a short funny Bengali kids story.";

            String apiUrl =
                    "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key="
                            + geminiApiKey;

            URL url = new URL(apiUrl);

            HttpURLConnection conn =
                    (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String body = """
            {
              "contents": [{
                "parts": [{
                  "text": "%s"
                }]
              }]
            }
            """.formatted(prompt);

            conn.getOutputStream().write(body.getBytes());

            BufferedReader br =
                    new BufferedReader(
                            new InputStreamReader(conn.getInputStream()));

            String line;

            StringBuilder response = new StringBuilder();

            while ((line = br.readLine()) != null) {
                response.append(line);
            }

            br.close();

            return response.toString();

        } catch (Exception e) {
            return e.getMessage();
        }
    }
}
