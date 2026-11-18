package com.tlim.scraper;

import com.tlim.server.ServerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Imports the official world list on the first start, so characters can be created right away.
 * Failure never blocks startup — an admin can retry with POST /api/admin/scrape/servers.
 */
@Component
@ConditionalOnProperty(name = "tlim.scraper.servers-on-startup", havingValue = "true", matchIfMissing = true)
public class ServerBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ServerBootstrap.class);

    private final ServerRepository serverRepository;
    private final ServerScraper serverScraper;

    public ServerBootstrap(ServerRepository serverRepository, ServerScraper serverScraper) {
        this.serverRepository = serverRepository;
        this.serverScraper = serverScraper;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (serverRepository.count() > 0) return;
        try {
            serverScraper.scrapeAll();
        } catch (RuntimeException e) {
            log.warn("Could not import the world list on startup: {}", e.getMessage());
        }
    }
}
