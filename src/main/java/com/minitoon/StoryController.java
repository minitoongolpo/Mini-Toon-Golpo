package com.minitoon;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StoryController {

    @GetMapping("/generate-story")
    public String generateStory() {
        return "Story generation started successfully!";
    }
}
