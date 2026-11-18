package com.tlim.hunt.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonHuntParserTest {

    private final JsonHuntParser parser = new JsonHuntParser(new ObjectMapper());

    @Test
    void parsesRealSession() {
        ParsedHunt hunt = parser.parse(HuntLogFixtures.read("small.json"));

        assertThat(hunt.sessionStart()).isEqualTo(LocalDateTime.of(2026, 1, 23, 18, 13, 14));
        assertThat(hunt.sessionEnd()).isEqualTo(LocalDateTime.of(2026, 1, 23, 18, 24, 4));
        assertThat(hunt.duration()).isEqualTo("00:10h");
        assertThat(hunt.rawXp()).isEqualTo(17_701);
        assertThat(hunt.lootTotal()).isEqualTo(1_479);
        assertThat(hunt.supplies()).isEqualTo(4_543);

        assertThat(hunt.monsters()).containsExactly(
                new ParsedMonster("bog raider", 20),
                new ParsedMonster("carrion worm", 12),
                new ParsedMonster("rotworm", 21));
        assertThat(hunt.items()).containsExactly(
                new ParsedItem("gold coin", 725),
                new ParsedItem("great spirit potion", 1),
                new ParsedItem("belted cape", 1));
    }

    @Test
    void parsesLargeSession() {
        ParsedHunt hunt = parser.parse(HuntLogFixtures.read("large.json"));

        assertThat(hunt.monsters()).hasSize(46);
        assertThat(hunt.items()).hasSize(70);
        assertThat(hunt.xpWithBonus()).isEqualTo(1_988_682);
        assertThat(hunt.damage()).isEqualTo(1_593_444);
        assertThat(hunt.items()).contains(new ParsedItem("strong mana potion", 14));
    }

    @Test
    void emptySession() {
        ParsedHunt hunt = parser.parse(HuntLogFixtures.read("empty.json"));

        assertThat(hunt.items()).isEmpty();
        assertThat(hunt.monsters()).isEmpty();
    }

    @Test
    void rejectsMalformedJson() {
        assertThatThrownBy(() -> parser.parse("{not json"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMissingFields() {
        assertThatThrownBy(() -> parser.parse("{\"Loot\": \"10\"}"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
