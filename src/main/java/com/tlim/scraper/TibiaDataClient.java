package com.tlim.scraper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Reads the official game world list through the TibiaData API, an open JSON mirror of
 * tibia.com/community/?subtopic=worlds (tibia.com itself rejects automated clients).
 */
@Component
public class TibiaDataClient {

    // Hardcoded to prevent SSRF — must never be user-configurable
    private static final URI WORLDS_URL = URI.create("https://api.tibiadata.com/v4/worlds");

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JsonNode fetchWorlds() {
        HttpRequest request = HttpRequest.newBuilder(WORLDS_URL)
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();
        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new ScraperException("World list request failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ScraperException("World list request interrupted", e);
        }
        if (response.statusCode() != 200) {
            throw new ScraperException("World list request returned HTTP " + response.statusCode());
        }
        try {
            return objectMapper.readTree(response.body());
        } catch (IOException e) {
            throw new ScraperException("World list response is not valid JSON", e);
        }
    }
}
