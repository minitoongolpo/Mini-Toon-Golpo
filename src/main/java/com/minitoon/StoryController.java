package com.minitoon;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

@RestController
public class StoryController {

    @Value("${GEMINI_API_KEY}")
    private String geminiApiKey;

    @GetMapping("/generate-story")
    public String generateStory() {

        try {

            URL url = new URL(
                    "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key="
                            + geminiApiKey);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String jsonInput = """
                    {
                      "contents": [{
                        "parts": [{
                          "text": "Write a short funny Bengali kids story."
                        }]
                      }]
                    }
                    """;

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInput.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            Scanner scanner = new Scanner(conn.getInputStream());
            StringBuilder response = new StringBuilder();

            while (scanner.hasNext()) {
                response.append(scanner.nextLine());
            }

            scanner.close();

            return response.toString();

        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}
