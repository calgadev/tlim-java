package com.tlim.scraper.dto;

import com.tlim.scraper.ScraperResult;
import java.time.Instant;

public record ScrapeStatusResponse(
        String status,
        Instant startedAt,
        Instant completedAt,
        ScraperResult result
) {}
