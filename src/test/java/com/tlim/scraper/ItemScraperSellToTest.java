package com.tlim.scraper;

import com.tlim.scraper.ItemScraper.ParsedBuyer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Inputs are real "sellto" / "npcvalue" values from TibiaWiki item pages. */
class ItemScraperSellToTest {

    @Test
    void explicitPricesOverrideNpcValue() {
        // Dragon Shield
        assertThat(ItemScraper.parseSellTo("H.L.: 115, Nah'Bob, Shanar: 360", "4000"))
                .containsExactly(
                        new ParsedBuyer("H.L.", 115),
                        new ParsedBuyer("Nah'Bob", 4000),
                        new ParsedBuyer("Shanar", 360));
    }

    @Test
    void namesWithoutPriceUseNpcValue() {
        // Wyvern Talisman
        assertThat(ItemScraper.parseSellTo("Fiona, Yasir", "265"))
                .containsExactly(new ParsedBuyer("Fiona", 265), new ParsedBuyer("Yasir", 265));
    }

    @Test
    void colonWithoutSpace() {
        // Minotaur Leather
        assertThat(ItemScraper.parseSellTo("Inigo, Tom:12", "80"))
                .containsExactly(new ParsedBuyer("Inigo", 80), new ParsedBuyer("Tom", 12));
    }

    @Test
    void accessConditionAfterSemicolonIsDropped() {
        // Soul Orb
        assertThat(ItemScraper.parseSellTo("The Bone Master;bob-rank1, The Dream Master;nk-rank1", "25"))
                .containsExactly(new ParsedBuyer("The Bone Master", 25), new ParsedBuyer("The Dream Master", 25));
    }

    @Test
    void noBuyers() {
        assertThat(ItemScraper.parseSellTo("--", "100")).isEmpty();   // Platinum Coin
        assertThat(ItemScraper.parseSellTo(null, "100")).isEmpty();
        assertThat(ItemScraper.parseSellTo("", "100")).isEmpty();
    }

    @Test
    void entriesWithoutUsablePriceAreSkipped() {
        assertThat(ItemScraper.parseSellTo("Rudolph", "0")).isEmpty();
        assertThat(ItemScraper.parseSellTo("Rudolph", "?")).isEmpty();
        assertThat(ItemScraper.parseSellTo("Rudolph, Yaman: 50", "?"))
                .containsExactly(new ParsedBuyer("Yaman", 50));
    }

    @Test
    void wikiLinksAreRemovedAndDuplicatesKeepHighestPrice() {
        assertThat(ItemScraper.parseSellTo("[[Yaman]]: 40, Yaman: 50", "10"))
                .containsExactly(new ParsedBuyer("Yaman", 50));
    }

    @Test
    void actualNameIsLowercasedWithDisplayNameFallback() {
        assertThat(ItemScraper.resolveActualName("dragon ham", "Dragon Ham")).isEqualTo("dragon ham");
        assertThat(ItemScraper.resolveActualName(null, "Dragon Ham")).isEqualTo("dragon ham");
        assertThat(ItemScraper.resolveActualName("?", "Rotworm")).isEqualTo("rotworm");
    }
}
