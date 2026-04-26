package com.tlim.scraper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class WikiHttpClient {

    private static final Logger log = LoggerFactory.getLogger(WikiHttpClient.class);

    private final ScraperProperties scraperProperties;

    public WikiHttpClient(ScraperProperties scraperProperties) {
        this.scraperProperties = scraperProperties;
    }

    public Document fetch(String url) {
        try {
            Thread.sleep(scraperProperties.getDelayMs());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ScraperException("Scraper thread interrupted during rate-limit delay", e);
        }
        log.debug("Fetching: {}", url);
        try {
            return Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(10000)
                    .get();
        } catch (IOException e) {
            throw new ScraperException("HTTP fetch failed for: " + url, e);
        }
    }
}
