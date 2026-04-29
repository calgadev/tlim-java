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
import java.util.Optional;
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

    public Optional<JsonNode> get(Map<String, String> params) {
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

        int status = response.statusCode();
        if (status == 403 || status == 429) {
            // Derive a human-readable label from params for audit logging
            String pageRef = params.getOrDefault("page", params.getOrDefault("cmtitle", url));
            if (status == 429) {
                log.warn("HTTP {} for '{}' — consider increasing tlim.scraper.delay-ms", status, pageRef);
            } else {
                log.warn("HTTP {} for '{}'", status, pageRef);
            }
            return Optional.empty();
        }
        if (status < 200 || status >= 300) {
            throw new ScraperException("Non-2xx response " + status + " for URL: " + url);
        }

        try {
            return Optional.of(objectMapper.readTree(response.body()));
        } catch (IOException e) {
            throw new ScraperException("Failed to parse JSON response for URL: " + url, e);
        }
    }
}
