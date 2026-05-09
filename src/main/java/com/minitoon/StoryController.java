package com.minitoon;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StoryController {

    @Value("${GEMINI_API_KEY:NOT_FOUND}")
    private String geminiApiKey;

    @GetMapping("/generate-story")
    public String generateStory() {

        if (geminiApiKey.equals("NOT_FOUND")) {
            return "Gemini API Key Missing!";
        }

        return "Story Generated Successfully using Gemini AI!";
    }
}
