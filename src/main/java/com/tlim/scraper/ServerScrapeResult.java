package com.tlim.scraper;

public record ServerScrapeResult(
        int created,
        int updated,
        int deactivated,
        int skipped
) {}
