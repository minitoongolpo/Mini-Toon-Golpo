package com.minitoon.service.ai;

import com.minitoon.config.AppProperties;
import com.minitoon.exception.AiServiceException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/**
 * ElevenLabs Service - Generates Bengali narration voice
 * Uses ElevenLabs API with multilingual v2 model supporting 29+ languages
 * Bengali (ben) is supported in Multilingual v2 and Eleven v3 models [^27^]
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ElevenLabsService {

    private final WebClient.Builder webClientBuilder;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    private static final String TTS_ENDPOINT = "/text-to-speech/%s";
    private static final String VOICE_ENDPOINT = "/voices";

    /**
     * Generate narration audio for a single scene
     * Uses eleven_multilingual_v2 which supports Bengali [^24^]
     */
    public String generateNarration(String text, String outputPath) {
        log.info("Generating narration for text ({} chars)", text.length());

        AppProperties.ElevenLabsProperties elevenlabs = appProperties.getAi().getElevenlabs();
        String endpoint = String.format(TTS_ENDPOINT, elevenlabs.getVoiceId());

        try {
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("text", text);
            requestBody.put("model_id", elevenlabs.getModelId());

            // Voice settings optimized for clarity
            ObjectNode voiceSettings = requestBody.putObject("voice_settings");
            voiceSettings.put("stability", 0.5);
            voiceSettings.put("similarity_boost", 0.75);
            voiceSettings.put("style", 0.3);
            voiceSettings.put("use_speaker_boost", true);

            // Language hint for Bengali
            requestBody.put("language_code", "bn");

            byte[] audioData = webClientBuilder.build()
                    .post()
                    .uri(elevenlabs.getBaseUrl() + endpoint)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header("xi-api-key", elevenlabs.getApiKey())
                    .header("Accept", "audio/mpeg")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .retryWhen(Retry.backoff(elevenlabs.getMaxRetries(), Duration.ofSeconds(2))
                            .filter(throwable -> {
                                log.warn("ElevenLabs API call failed, retrying: {}", throwable.getMessage());
                                return true;
                            }))
                    .block(Duration.ofSeconds(elevenlabs.getTimeout()));

            if (audioData == null || audioData.length == 0) {
                throw new AiServiceException("ElevenLabs", "Empty audio response", 0);
            }

            Path path = Path.of(outputPath);
            Files.createDirectories(path.getParent());
            Files.write(path, audioData);

            log.info("Narration saved: {} ({} bytes)", outputPath, audioData.length);
            return outputPath;

        } catch (AiServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new AiServiceException("ElevenLabs", "TTS generation failed: " + e.getMessage(), e, 0);
        }
    }

    /**
     * Generate narration for multiple scenes combined
     */
    public String generateCombinedNarration(java.util.List<String> texts, String outputPath) {
        log.info("Generating combined narration for {} scenes", texts.size());

        StringBuilder combined = new StringBuilder();
        for (int i = 0; i < texts.size(); i++) {
            combined.append(texts.get(i));
            if (i < texts.size() - 1) {
                combined.append(". ");
            }
        }

        return generateNarration(combined.toString(), outputPath);
    }

    /**
     * Get available voices - useful for finding Bengali-compatible voices
     */
    public JsonNode getVoices() {
        AppProperties.ElevenLabsProperties elevenlabs = appProperties.getAi().getElevenlabs();

        try {
            return webClientBuilder.build()
                    .get()
                    .uri(elevenlabs.getBaseUrl() + VOICE_ENDPOINT)
                    .header("xi-api-key", elevenlabs.getApiKey())
                    .header("Accept", "application/json")
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(Duration.ofSeconds(30));
        } catch (Exception e) {
            log.error("Failed to fetch voices: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Generate voice with custom settings for kids-friendly tone
     * Uses lower stability for more expressiveness [^24^]
     */
    public String generateKidsNarration(String text, String outputPath) {
        log.info("Generating kids-friendly narration");

        AppProperties.ElevenLabsProperties elevenlabs = appProperties.getAi().getElevenlabs();
        String endpoint = String.format(TTS_ENDPOINT, elevenlabs.getVoiceId());

        try {
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("text", text);
            requestBody.put("model_id", elevenlabs.getModelId());
            requestBody.put("language_code", "bn");

            // Kids-friendly voice settings - warmer, more expressive [^24^]
            ObjectNode voiceSettings = requestBody.putObject("voice_settings");
            voiceSettings.put("stability", 0.35);  // More variation for expressiveness
            voiceSettings.put("similarity_boost", 0.80);
            voiceSettings.put("style", 0.5);       // More style for storytelling
            voiceSettings.put("use_speaker_boost", true);

            byte[] audioData = webClientBuilder.build()
                    .post()
                    .uri(elevenlabs.getBaseUrl() + endpoint)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header("xi-api-key", elevenlabs.getApiKey())
                    .header("Accept", "audio/mpeg")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .retryWhen(Retry.backoff(elevenlabs.getMaxRetries(), Duration.ofSeconds(2))
                            .filter(throwable -> {
                                log.warn("ElevenLabs kids narration failed, retrying: {}", throwable.getMessage());
                                return true;
                            }))
                    .block(Duration.ofSeconds(elevenlabs.getTimeout()));

            if (audioData == null || audioData.length == 0) {
                throw new AiServiceException("ElevenLabs", "Empty audio response for kids narration", 0);
            }

            Path path = Path.of(outputPath);
            Files.createDirectories(path.getParent());
            Files.write(path, audioData);

            log.info("Kids narration saved: {} ({} bytes)", outputPath, audioData.length);
            return outputPath;

        } catch (AiServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new AiServiceException("ElevenLabs", "Kids narration failed: " + e.getMessage(), e, 0);
        }
    }
}
