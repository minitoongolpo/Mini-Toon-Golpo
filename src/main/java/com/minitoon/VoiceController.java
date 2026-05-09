package com.minitoon;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VoiceController {

    @Value("${ELEVENLABS_API_KEY}")
    private String elevenApiKey;

    @Value("${ELEVENLABS_VOICE_ID}")
    private String voiceId;

    @GetMapping("/generate-voice")
    public String generateVoice() {

        return "Voice generation ready! Voice ID = " + voiceId;
    }
}
