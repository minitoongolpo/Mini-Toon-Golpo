package com.minitoon;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ImageController {

    @Value("${LEONARDO_API_KEY:NOT_FOUND}")
    private String leonardoKey;

    @GetMapping("/generate-image")
    public String generateImage() {

        return "Leonardo AI Ready!";
    }
}
