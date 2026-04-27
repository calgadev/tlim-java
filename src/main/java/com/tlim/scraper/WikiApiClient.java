package com.tlim.scraper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.StringJoiner;

@Component
public class WikiApiClient {

    private static final Logger log = LoggerFactory.getLogger(WikiApiClient.class);
    // Hardcoded to prevent SSRF — must never be user-configurable
    private static final String BASE_URL = "https://tibia.fandom.com/api.php";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ScraperProperties scraperProperties;

    public WikiApiClient(ScraperProperties scraperProperties) {
        this.scraperProperties = scraperProperties;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public JsonNode get(Map<String, String> params) {
        StringJoiner queryString = new StringJoiner("&");
        for (Map.Entry<String, String> entry : params.entrySet()) {
            queryString.add(
                URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) + "=" +
                URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8)
            );
        }
        queryString.add("format=json");
        queryString.add("formatversion=2");

        String url = BASE_URL + "?" + queryString;
        log.debug("Fetching: {}", url);

        try {
            Thread.sleep(scraperProperties.getDelayMs());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ScraperException("Scraper thread interrupted", e);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new ScraperException("HTTP request failed for URL: " + url, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ScraperException("HTTP request interrupted for URL: " + url, e);
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new ScraperException("Non-2xx response " + response.statusCode() + " for URL: " + url);
        }

        try {
            return objectMapper.readTree(response.body());
        } catch (IOException e) {
            throw new ScraperException("Failed to parse JSON response for URL: " + url, e);
        }
    }
}
