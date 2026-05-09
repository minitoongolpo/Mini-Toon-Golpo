package com.minitoon;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/")
    public String home() {
        return "Mini Toon Golpo API Running!";
    }

    @GetMapping("/health")
    public String health() {
        return "UP";
    }

    @GetMapping("/api/health")
    public String apiHealth() {
        return "API UP";
    }
}
