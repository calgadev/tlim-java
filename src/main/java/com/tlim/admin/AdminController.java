package com.tlim.admin;

import com.tlim.scraper.ScraperService;
import com.tlim.scraper.dto.ScrapeStatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin")
public class AdminController {

    private final ScraperService scraperService;

    // compareAndSet makes the check-and-set atomic — a plain if (!isScraping) is a race condition
    private final AtomicBoolean isScraping = new AtomicBoolean(false);

    // volatile ensures the completion callback's write is visible to threads serving GET /status
    private volatile ScrapeStatusResponse lastStatus = new ScrapeStatusResponse("IDLE", null, null, null);

    public AdminController(ScraperService scraperService) {
        this.scraperService = scraperService;
    }

    @PostMapping("/scrape")
    @Operation(summary = "Trigger a full TibiaWiki scrape asynchronously")
    @ApiResponses({
        @ApiResponse(responseCode = "202", description = "Scrape started"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden — ADMIN role required"),
        @ApiResponse(responseCode = "409", description = "Scrape already running")
    })
    public ResponseEntity<Object> startScrape() {
        if (!isScraping.compareAndSet(false, true)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("status", 409, "error", "Conflict", "message", "Scrape already running"));
        }

        Instant startedAt = Instant.now();
        lastStatus = new ScrapeStatusResponse("RUNNING", startedAt, null, null);

        // supplyAsync to capture the ScraperResult; whenComplete runs on both success and failure
        CompletableFuture.supplyAsync(scraperService::runFullScrape)
            .whenComplete((result, ex) -> {
                // Guaranteed to run even if runFullScrape throws — prevents permanent isScraping lock
                isScraping.set(false);
                Instant completedAt = Instant.now();
                if (ex != null) {
                    // Safe summary only — exception message and stack trace must never appear in the response
                    lastStatus = new ScrapeStatusResponse("FAILED", startedAt, completedAt, null);
                } else {
                    lastStatus = new ScrapeStatusResponse("COMPLETED", startedAt, completedAt, result);
                }
            });

        return ResponseEntity.accepted().body(lastStatus);
    }

    @GetMapping("/scrape/status")
    @Operation(summary = "Get the status of the most recent scrape run")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Status returned (IDLE, RUNNING, COMPLETED, or FAILED)"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden — ADMIN role required")
    })
    public ResponseEntity<ScrapeStatusResponse> getScrapeStatus() {
        return ResponseEntity.ok(lastStatus);
    }
}
