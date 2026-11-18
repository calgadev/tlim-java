package com.tlim.scraper;

public record ScraperResult(
        // null when the world list could not be fetched; items and creatures are still scraped
        ServerScrapeResult servers,
        int itemsScraped,
        int itemsFailed,
        int creaturesScraped,
        int creaturesFailed,
        long durationSeconds
) {}
