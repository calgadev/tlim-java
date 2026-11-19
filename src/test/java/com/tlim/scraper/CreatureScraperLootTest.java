package com.tlim.scraper;

import com.tlim.scraper.CreatureScraper.LootEntry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Loot amounts as written on TibiaWiki creature pages, including the open-ended forms. */
class CreatureScraperLootTest {

    @Test
    void parsesFixedAndRangedAmounts() {
        String block = "{{Loot2\n"
                + "|{{Loot Item|0-48|Gold Coin}}\n"
                + "|{{Loot Item|3|Worm|semi-rare}}\n"
                + "|{{Loot Item|Belted Cape|rare}}\n"
                + "}}";

        assertThat(CreatureScraper.parseLootEntries(block)).containsExactly(
                new LootEntry("Gold Coin", null, 0, 48),
                new LootEntry("Worm", "semi-rare", 3, 3),
                new LootEntry("Belted Cape", "rare", 1, 1));
    }

    @Test
    void openEndedAmountsAreNotMistakenForItemNames() {
        // Seen on 'Zomba', 'Roaring Lion' and 'Imperial'
        String block = "{{Loot2\n"
                + "|{{Loot Item|1-29+|Gold Coin}}\n"
                + "|{{Loot Item|1-?|Platinum Coin|rare}}\n"
                + "|{{Loot Item|1+|Meat}}\n"
                + "|{{Loot Item|?|Ham}}\n"
                + "|{{Loot Item|0-120?|Gold Coin}}\n"        // 'Tzumrah the Dazzler'
                + "|{{Loot Item|1-2?|Fox Tail|semi-rare}}\n"  // 'Fox'
                + "}}";

        assertThat(CreatureScraper.parseLootEntries(block)).containsExactly(
                new LootEntry("Gold Coin", null, 1, 29),
                new LootEntry("Platinum Coin", "rare", 1, 1),
                new LootEntry("Meat", null, 1, 1),
                new LootEntry("Ham", null, 1, 1),
                new LootEntry("Gold Coin", null, 0, 120),
                new LootEntry("Fox Tail", "semi-rare", 1, 2));
    }

    @Test
    void amountRangeEdgeCases() {
        assertThat(CreatureScraper.parseAmountRange("1-4+")).containsExactly(1, 4);
        assertThat(CreatureScraper.parseAmountRange("2 - 5")).containsExactly(2, 5);
        assertThat(CreatureScraper.parseAmountRange("5-?")).containsExactly(5, 5);
        assertThat(CreatureScraper.parseAmountRange("4-197?")).containsExactly(4, 197);
        assertThat(CreatureScraper.parseAmountRange("3?")).containsExactly(3, 3);
        assertThat(CreatureScraper.parseAmountRange(null)).containsExactly(1, 1);
        assertThat(CreatureScraper.parseAmountRange("abc")).containsExactly(1, 1);
    }
}
