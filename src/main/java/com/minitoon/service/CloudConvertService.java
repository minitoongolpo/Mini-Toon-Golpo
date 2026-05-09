package com.minitoon.service;

import com.minitoon.config.AppProperties;
import com.minitoon.exception.MiniToonException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/**
 * CloudConvert Service - File conversion and public URL generation
 * Uses CloudConvert API v2 for video processing and temporary public URLs
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CloudConvertService {

    private final WebClient.Builder webClientBuilder;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    private static final String JOBS_ENDPOINT = "/jobs";

    /**
     * Upload a local file to CloudConvert and get a temporary public URL.
     * Essential for Instagram/Facebook which require public URLs.
     */
    public String uploadAndGetPublicUrl(String localFilePath) {
        log.info("Uploading file to CloudConvert: {}", localFilePath);

        try {
            byte[] fileData = Files.readAllBytes(Path.of(localFilePath));
            String filename = Path.of(localFilePath).getFileName().toString();

            // Create job with import/upload + export/url tasks
            ObjectNode jobRequest = objectMapper.createObjectNode();
            ArrayNode tasks = jobRequest.putArray("tasks");

            ObjectNode importTask = tasks.addObject();
            importTask.put("name", "upload-file");
            importTask.put("operation", "import/upload");

            ObjectNode exportTask = tasks.addObject();
            exportTask.put("name", "export-file");
            exportTask.put("operation", "export/url");
            exportTask.put("input", "upload-file");

            JsonNode response = webClientBuilder.build()
                    .post()
                    .uri(appProperties.getCloudconvert().getBaseUrl() + JOBS_ENDPOINT)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header("Authorization", "Bearer " + appProperties.getCloudconvert().getApiKey())
                    .bodyValue(jobRequest)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(5)))
                    .block(Duration.ofSeconds(60));

            String jobId = response.path("data").path("id").asText();

            // Get upload URL and upload file
            JsonNode jobStatus = getJobStatus(jobId);
            JsonNode uploadForm = jobStatus.path("data").path("tasks").get(0).path("result").path("form");
            String uploadUrl = uploadForm.path("url").asText();

            // Upload actual file data
            uploadFileToUrl(uploadUrl, fileData, filename, uploadForm);

            // Poll for export URL
            return pollForExportUrl(jobId);

        } catch (Exception e) {
            throw new MiniToonException("CloudConvert upload failed: " + e.getMessage());
        }
    }

    private void uploadFileToUrl(String url, byte[] data, String filename, JsonNode formData) {
        webClientBuilder.build()
                .post()
                .uri(url)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(data)
                .retrieve()
                .bodyToMono(String.class)
                .block(Duration.ofSeconds(120));
    }

    private String pollForExportUrl(String jobId) {
        for (int i = 0; i < 60; i++) {
            try {
                Thread.sleep(5000);
                JsonNode status = getJobStatus(jobId);
                String state = status.path("data").path("status").asText();

                if ("finished".equals(state)) {
                    JsonNode files = status.path("data").path("tasks").get(1).path("result").path("files");
                    if (files.isArray() && files.size() > 0) {
                        return files.get(0).path("url").asText();
                    }
                } else if ("error".equals(state)) {
                    throw new MiniToonException("CloudConvert job failed");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new MiniToonException("Polling interrupted");
            }
        }
        throw new MiniToonException("CloudConvert job timed out");
    }

    private JsonNode getJobStatus(String jobId) {
        return webClientBuilder.build()
                .get()
                .uri(appProperties.getCloudconvert().getBaseUrl() + JOBS_ENDPOINT + "/" + jobId)
                .header("Authorization", "Bearer " + appProperties.getCloudconvert().getApiKey())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block(Duration.ofSeconds(30));
    }
}
