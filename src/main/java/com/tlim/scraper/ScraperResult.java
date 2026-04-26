package com.tlim.scraper;

public record ScraperResult(
        int itemsScraped,
        int itemsFailed,
        int creaturesScraped,
        int creaturesFailed,
        long durationSeconds
) {}
