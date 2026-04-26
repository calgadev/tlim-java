package com.tlim.scraper;

import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class ScraperService {

    private final ItemScraper itemScraper;
    private final CreatureScraper creatureScraper;

    public ScraperService(ItemScraper itemScraper, CreatureScraper creatureScraper) {
        this.itemScraper = itemScraper;
        this.creatureScraper = creatureScraper;
    }

    public ScraperResult runFullScrape() {
        Instant start = Instant.now();

        int[] itemCounts = itemScraper.scrapeAll();
        int[] creatureCounts = creatureScraper.scrapeAll();

        long durationSeconds = Instant.now().getEpochSecond() - start.getEpochSecond();

        return new ScraperResult(
                itemCounts[0],
                itemCounts[1],
                creatureCounts[0],
                creatureCounts[1],
                durationSeconds
        );
    }
}
