package com.tlim.hunt.parser;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TextHuntParserTest {

    private final TextHuntParser parser = new TextHuntParser();

    @Test
    void parsesRealSessionWithCrlfLineEndings() {
        ParsedHunt hunt = parser.parse(HuntLogFixtures.read("small.txt"));

        assertThat(hunt.sessionStart()).isEqualTo(LocalDateTime.of(2026, 1, 23, 18, 13, 14));
        assertThat(hunt.sessionEnd()).isEqualTo(LocalDateTime.of(2026, 1, 23, 18, 24, 1));
        assertThat(hunt.duration()).isEqualTo("00:10h");
        assertThat(hunt.rawXp()).isEqualTo(17_701);
        assertThat(hunt.xpWithBonus()).isEqualTo(26_551);
        assertThat(hunt.lootTotal()).isEqualTo(1_479);
        assertThat(hunt.supplies()).isEqualTo(4_543);
        assertThat(hunt.damage()).isEqualTo(31_183);
        assertThat(hunt.healing()).isEqualTo(6_401);

        assertThat(hunt.monsters()).containsExactly(
                new ParsedMonster("bog raider", 20),
                new ParsedMonster("carrion worm", 12),
                new ParsedMonster("rotworm", 21));
        // Articles are stripped so names match the in-game item name
        assertThat(hunt.items()).containsExactly(
                new ParsedItem("gold coin", 725),
                new ParsedItem("great spirit potion", 1),
                new ParsedItem("belted cape", 1));
    }

    @Test
    void emptySessionWithNoneMarkersHasNoEntries() {
        ParsedHunt hunt = parser.parse(HuntLogFixtures.read("empty.txt"));

        assertThat(hunt.items()).isEmpty();
        assertThat(hunt.monsters()).isEmpty();
        assertThat(hunt.lootTotal()).isZero();
    }

    @Test
    void parsesLargeSession() {
        ParsedHunt hunt = parser.parse(HuntLogFixtures.read("large.txt"));

        assertThat(hunt.monsters()).hasSize(48);
        assertThat(hunt.items()).hasSize(29);
        assertThat(hunt.damage()).isEqualTo(1_165_883);
        assertThat(hunt.items()).contains(
                new ParsedItem("gold coin", 31_401),
                new ParsedItem("earflap", 124),   // "an earflap"
                new ParsedItem("seeds", 1));      // no article
    }

    @Test
    void acceptsThousandsSeparatorInQuantity() {
        String log = HuntLogFixtures.read("small.txt").replace("725x a gold coin", "1,725x a gold coin");

        assertThat(parser.parse(log).items()).contains(new ParsedItem("gold coin", 1_725));
    }

    @Test
    void reportsMissingHeaderFields() {
        assertThatThrownBy(() -> parser.parse("Killed Monsters:\n  1x rat\n"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sessionStart")
                .hasMessageContaining("lootTotal");
    }
}
