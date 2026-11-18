package com.tlim.scraper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class ScraperService {

    private static final Logger log = LoggerFactory.getLogger(ScraperService.class);

    private final ServerScraper serverScraper;
    private final ItemScraper itemScraper;
    private final CreatureScraper creatureScraper;

    public ScraperService(ServerScraper serverScraper, ItemScraper itemScraper, CreatureScraper creatureScraper) {
        this.serverScraper = serverScraper;
        this.itemScraper = itemScraper;
        this.creatureScraper = creatureScraper;
    }

    public ServerScrapeResult scrapeServers() {
        return serverScraper.scrapeAll();
    }

    public ScraperResult runFullScrape() {
        Instant start = Instant.now();

        // Servers come from a different source; a failure there must not cancel the wiki crawl
        ServerScrapeResult servers = null;
        try {
            servers = serverScraper.scrapeAll();
        } catch (RuntimeException e) {
            log.warn("World list scrape failed: {}", e.getMessage());
        }

        // Items before creatures — creature loot links to existing items
        int[] itemCounts = itemScraper.scrapeAll();
        int[] creatureCounts = creatureScraper.scrapeAll();

        long durationSeconds = Instant.now().getEpochSecond() - start.getEpochSecond();

        return new ScraperResult(
                servers,
                itemCounts[0],
                itemCounts[1],
                creatureCounts[0],
                creatureCounts[1],
                durationSeconds
        );
    }
}
