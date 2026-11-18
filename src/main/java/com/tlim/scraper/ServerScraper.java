package com.tlim.scraper;

import com.fasterxml.jackson.databind.JsonNode;
import com.tlim.server.PvpType;
import com.tlim.server.Server;
import com.tlim.server.ServerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Synchronises the servers table with the official game world list.
 * Worlds are upserted by name; worlds missing from the list are flagged inactive, never deleted,
 * because characters may still reference them.
 */
@Component
public class ServerScraper {

    private static final Logger log = LoggerFactory.getLogger(ServerScraper.class);

    private static final Map<String, PvpType> PVP_TYPES = Map.of(
            "optional pvp", PvpType.OPTIONAL_PVP,
            "open pvp", PvpType.OPEN_PVP,
            "retro open pvp", PvpType.RETRO_OPEN_PVP,
            "retro hardcore pvp", PvpType.RETRO_HARDCORE_PVP,
            "hardcore pvp", PvpType.HARDCORE_PVP);

    private final TibiaDataClient tibiaDataClient;
    private final ServerRepository serverRepository;
    private final TransactionTemplate transactionTemplate;

    public ServerScraper(TibiaDataClient tibiaDataClient, ServerRepository serverRepository,
                         TransactionTemplate transactionTemplate) {
        this.tibiaDataClient = tibiaDataClient;
        this.serverRepository = serverRepository;
        this.transactionTemplate = transactionTemplate;
    }

    record ParsedWorld(String name, PvpType pvpType, String location) {}

    record ParseResult(List<ParsedWorld> worlds, List<String> unknownPvpTypes) {}

    public ServerScrapeResult scrapeAll() {
        ParseResult parsed = parseWorlds(tibiaDataClient.fetchWorlds());
        if (parsed.worlds().isEmpty()) {
            // An empty list is almost certainly an API problem — never deactivate every server because of it
            throw new ScraperException("World list is empty — servers left unchanged");
        }
        parsed.unknownPvpTypes().forEach(w -> log.warn("Skipping world with unknown PvP type: {}", w));

        // All-or-nothing: a failure mid-way must not leave half the worlds deactivated
        ServerScrapeResult result = transactionTemplate.execute(status -> apply(parsed.worlds()));
        log.info("World list synchronised: {} created, {} updated, {} deactivated, {} skipped",
                result.created(), result.updated(), result.deactivated(), parsed.unknownPvpTypes().size());
        return new ServerScrapeResult(result.created(), result.updated(), result.deactivated(),
                parsed.unknownPvpTypes().size());
    }

    private ServerScrapeResult apply(List<ParsedWorld> worlds) {
        int created = 0;
        int updated = 0;
        Set<Long> seen = new HashSet<>();

        for (ParsedWorld world : worlds) {
            Optional<Server> existing = serverRepository.findFirstByNameIgnoreCase(world.name());
            Server server = existing.orElseGet(Server::new);
            server.setName(world.name());
            server.setPvpType(world.pvpType());
            server.setLocation(world.location());
            server.setActive(true);
            server = serverRepository.save(server);
            seen.add(server.getId());
            if (existing.isPresent()) updated++; else created++;
        }

        int deactivated = 0;
        for (Server server : serverRepository.findByActiveTrueOrderByNameAsc()) {
            if (!seen.contains(server.getId())) {
                server.setActive(false);
                serverRepository.save(server);
                deactivated++;
                log.info("World '{}' is no longer in the official list — marked inactive", server.getName());
            }
        }
        return new ServerScrapeResult(created, updated, deactivated, 0);
    }

    /** Extracts worlds from a TibiaData v4 /worlds response. */
    static ParseResult parseWorlds(JsonNode root) {
        List<ParsedWorld> worlds = new ArrayList<>();
        List<String> unknown = new ArrayList<>();
        for (JsonNode node : root.path("worlds").path("regular_worlds")) {
            String name = node.path("name").asText("").trim();
            if (name.isEmpty()) continue;
            String pvpLabel = node.path("pvp_type").asText("").trim();
            PvpType pvpType = PVP_TYPES.get(pvpLabel.toLowerCase(Locale.ROOT));
            if (pvpType == null) {
                unknown.add(name + " (" + pvpLabel + ")");
                continue;
            }
            String location = node.path("location").asText("").trim();
            worlds.add(new ParsedWorld(name, pvpType, location.isEmpty() ? null : location));
        }
        return new ParseResult(worlds, unknown);
    }
}
