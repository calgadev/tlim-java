package com.tlim.scraper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tlim.TestData;
import com.tlim.scraper.ServerScraper.ParsedWorld;
import com.tlim.server.PvpType;
import com.tlim.server.Server;
import com.tlim.server.ServerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ServerScraperTest {

    @Mock TibiaDataClient tibiaDataClient;
    @Mock ServerRepository serverRepository;
    @Mock TransactionTemplate transactionTemplate;

    private ServerScraper scraper;
    private final List<Server> table = new ArrayList<>();
    private long nextId = 100;

    // Trimmed copy of a real TibiaData v4 /worlds response: one world per PvP type
    private static JsonNode fixture() throws IOException {
        try (InputStream in = ServerScraperTest.class.getResourceAsStream("/scraper/tibiadata-worlds.json")) {
            return new ObjectMapper().readTree(in);
        }
    }

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        scraper = new ServerScraper(tibiaDataClient, serverRepository, transactionTemplate);
        when(transactionTemplate.execute(any())).thenAnswer(inv ->
                ((TransactionCallback<Object>) inv.getArgument(0)).doInTransaction(null));

        when(serverRepository.findFirstByNameIgnoreCase(anyString())).thenAnswer(inv -> table.stream()
                .filter(s -> s.getName().equalsIgnoreCase(inv.getArgument(0))).findFirst());
        when(serverRepository.save(any(Server.class))).thenAnswer(inv -> {
            Server s = inv.getArgument(0);
            if (s.getId() == null) {
                s.setId(nextId++);
                table.add(s);
            }
            return s;
        });
        when(serverRepository.findByActiveTrueOrderByNameAsc())
                .thenAnswer(inv -> table.stream().filter(Server::isActive).toList());
    }

    @Test
    void parsesEveryPvpTypeFromTheRealResponse() throws IOException {
        var result = ServerScraper.parseWorlds(fixture());

        assertThat(result.unknownPvpTypes()).isEmpty();
        assertThat(result.worlds()).containsExactly(
                new ParsedWorld("Antica", PvpType.OPEN_PVP, "Europe"),
                new ParsedWorld("Astera", PvpType.OPTIONAL_PVP, "North America"),
                new ParsedWorld("Eclipta", PvpType.RETRO_OPEN_PVP, "Europe"),
                new ParsedWorld("Monstera", PvpType.RETRO_HARDCORE_PVP, "North America"),
                new ParsedWorld("Zuna", PvpType.HARDCORE_PVP, "Europe"));
    }

    @Test
    void unknownPvpTypeIsReportedNotGuessed() throws IOException {
        JsonNode json = new ObjectMapper().readTree("""
                {"worlds":{"regular_worlds":[
                  {"name":"Newworld","pvp_type":"Seasonal PvP","location":"Europe"},
                  {"name":"Antica","pvp_type":"Open PvP","location":"Europe"}]}}""");

        var result = ServerScraper.parseWorlds(json);

        assertThat(result.worlds()).extracting(ParsedWorld::name).containsExactly("Antica");
        assertThat(result.unknownPvpTypes()).containsExactly("Newworld (Seasonal PvP)");
    }

    @Test
    void firstRunCreatesEveryWorld() throws IOException {
        when(tibiaDataClient.fetchWorlds()).thenReturn(fixture());

        ServerScrapeResult result = scraper.scrapeAll();

        assertThat(result).isEqualTo(new ServerScrapeResult(5, 0, 0, 0));
        assertThat(table).extracting(Server::getName)
                .containsExactly("Antica", "Astera", "Eclipta", "Monstera", "Zuna");
    }

    @Test
    void rerunUpdatesExistingAndDeactivatesWorldsNoLongerListed() throws IOException {
        Server antica = TestData.server(1, "antica");          // different case, wrong PvP type
        Server closed = TestData.server(2, "Zunera");           // merged world, still referenced by characters
        table.add(antica);
        table.add(closed);
        when(tibiaDataClient.fetchWorlds()).thenReturn(fixture());

        ServerScrapeResult result = scraper.scrapeAll();

        assertThat(result).isEqualTo(new ServerScrapeResult(4, 1, 1, 0));
        assertThat(antica.getName()).isEqualTo("Antica");
        assertThat(antica.getPvpType()).isEqualTo(PvpType.OPEN_PVP);
        assertThat(antica.getLocation()).isEqualTo("Europe");
        assertThat(closed.isActive()).isFalse();
        verify(serverRepository, never()).delete(any());
    }

    @Test
    void reactivatesWorldThatCameBack() throws IOException {
        Server astera = TestData.server(1, "Astera");
        astera.setActive(false);
        table.add(astera);
        when(tibiaDataClient.fetchWorlds()).thenReturn(fixture());

        scraper.scrapeAll();

        assertThat(astera.isActive()).isTrue();
    }

    @Test
    void emptyListNeverDeactivatesEverything() throws IOException {
        table.add(TestData.server(1, "Antica"));
        when(tibiaDataClient.fetchWorlds()).thenReturn(new ObjectMapper().readTree("{\"worlds\":{}}"));

        assertThatThrownBy(() -> scraper.scrapeAll()).isInstanceOf(ScraperException.class);
        assertThat(table.get(0).isActive()).isTrue();
        verify(serverRepository, never()).save(any());
    }
}
