package com.tlim.scraper;

import com.tlim.creature.Creature;
import com.tlim.creature.CreatureLoot;
import com.tlim.creature.CreatureLootRepository;
import com.tlim.creature.CreatureRepository;
import com.tlim.item.Item;
import com.tlim.item.ItemRepository;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class CreatureScraper {

    private static final Logger log = LoggerFactory.getLogger(CreatureScraper.class);
    private static final String BASE_URL = "https://tibia.fandom.com";
    private static final String CREATURE_INDEX_URL = BASE_URL + "/wiki/Creatures";

    private final WikiHttpClient wikiHttpClient;
    private final CreatureRepository creatureRepository;
    private final CreatureLootRepository creatureLootRepository;
    private final ItemRepository itemRepository;
    private final TransactionTemplate transactionTemplate;

    public CreatureScraper(WikiHttpClient wikiHttpClient, CreatureRepository creatureRepository,
                           CreatureLootRepository creatureLootRepository, ItemRepository itemRepository,
                           TransactionTemplate transactionTemplate) {
        this.wikiHttpClient = wikiHttpClient;
        this.creatureRepository = creatureRepository;
        this.creatureLootRepository = creatureLootRepository;
        this.itemRepository = itemRepository;
        this.transactionTemplate = transactionTemplate;
    }

    // Returns int[] { creaturesScraped, creaturesFailed }
    public int[] scrapeAll() {
        int scraped = 0;
        int failed = 0;

        // Level 1: collect creature group page URLs from the Creatures index
        List<String> groupUrls = collectGroupUrls();
        log.info("Found {} creature groups", groupUrls.size());

        // Level 2: collect individual creature page URLs from each group
        Set<String> creatureUrls = new LinkedHashSet<>();
        for (String groupUrl : groupUrls) {
            creatureUrls.addAll(collectCreatureUrls(groupUrl));
        }
        log.info("Found {} unique creature pages to scrape", creatureUrls.size());

        // Level 3: scrape and upsert each creature page
        for (String creatureUrl : creatureUrls) {
            try {
                boolean wasCreature = scrapeAndUpsertCreature(creatureUrl);
                if (wasCreature) scraped++;
                // non-creature pages that slip through Level 2 are silently skipped (logged at DEBUG inside)
            } catch (Exception e) {
                log.warn("Failed to scrape creature page {}: {}", creatureUrl, e.getMessage());
                failed++;
            }
        }

        log.info("Creature scrape complete: {} scraped, {} failed", scraped, failed);
        return new int[]{scraped, failed};
    }

    private List<String> collectGroupUrls() {
        List<String> urls = new ArrayList<>();
        try {
            Document doc = wikiHttpClient.fetch(CREATURE_INDEX_URL);
            for (Element link : doc.select(".mw-parser-output a[href]")) {
                if (isValidWikiLink(link)) {
                    String resolved = safeUrl(link.attr("href"));
                    if (resolved != null) urls.add(resolved);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to collect creature group URLs: {}", e.getMessage());
        }
        return urls;
    }

    private List<String> collectCreatureUrls(String groupUrl) {
        List<String> urls = new ArrayList<>();
        try {
            Document doc = wikiHttpClient.fetch(groupUrl);
            for (Element link : doc.select(".mw-parser-output a[href]")) {
                if (isValidWikiLink(link)) {
                    String resolved = safeUrl(link.attr("href"));
                    if (resolved != null) urls.add(resolved);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to collect creature URLs from {}: {}", groupUrl, e.getMessage());
        }
        return urls;
    }

    private boolean isValidWikiLink(Element link) {
        String href = link.attr("href");
        return href.startsWith("/wiki/")
                && !href.contains("Category:")
                && !href.contains("List_of")
                && !href.contains("#")
                && !href.contains("Tibia.com")
                && !href.contains("File:");
    }

    /**
     * @return true if the page was a creature page that was scraped; false if skipped (non-creature)
     */
    private boolean scrapeAndUpsertCreature(String url) {
        Document doc = wikiHttpClient.fetch(url);

        Element titleEl = doc.selectFirst("h1.page-header__title");
        if (titleEl == null) throw new ScraperException("No page title found on: " + url);
        String name = titleEl.text().trim();
        if (name.isBlank()) throw new ScraperException("Blank page title on: " + url);

        Element infobox = doc.selectFirst(".infobox-wrapper");
        if (infobox == null) infobox = doc.selectFirst("table.infobox");

        // Skip pages that don't have a creature infobox (items, categories, etc. that slipped through)
        if (infobox == null || !isCreaturePage(infobox)) {
            log.debug("Skipping non-creature page: {}", url);
            return false;
        }

        String imageUrl = null;
        Element img = infobox.selectFirst("img");
        if (img != null) {
            imageUrl = img.hasAttr("data-src") ? img.attr("data-src") : img.attr("src");
            if (imageUrl != null && imageUrl.isBlank()) imageUrl = null;
        }

        Integer hp = parseInfoboxInt(infobox, "Hit Points", url);
        Integer experience = parseInfoboxInt(infobox, "Experience Points", url);

        // Resistance values: null if the row is absent on this creature's page
        Integer physicalResistance = parseResistance(getInfoboxValue(infobox, "Physical"));
        Integer fireResistance     = parseResistance(getInfoboxValue(infobox, "Fire"));
        Integer iceResistance      = parseResistance(getInfoboxValue(infobox, "Ice"));
        Integer energyResistance   = parseResistance(getInfoboxValue(infobox, "Energy"));
        Integer earthResistance    = parseResistance(getInfoboxValue(infobox, "Earth"));
        Integer deathResistance    = parseResistance(getInfoboxValue(infobox, "Death"));
        Integer holyResistance     = parseResistance(getInfoboxValue(infobox, "Holy"));
        Integer drownResistance    = parseResistance(getInfoboxValue(infobox, "Drown"));

        List<ParsedLoot> lootEntries = parseLootTable(doc);

        // Capture finals for lambda
        final String fName = name;
        final String fImageUrl = imageUrl;
        final Integer fHp = hp;
        final Integer fExperience = experience;
        final Integer fPhysical = physicalResistance;
        final Integer fFire = fireResistance;
        final Integer fIce = iceResistance;
        final Integer fEnergy = energyResistance;
        final Integer fEarth = earthResistance;
        final Integer fDeath = deathResistance;
        final Integer fHoly = holyResistance;
        final Integer fDrown = drownResistance;

        transactionTemplate.executeWithoutResult(status -> {
            Creature creature = creatureRepository.findByName(fName).orElseGet(Creature::new);
            creature.setName(fName);
            creature.setWikiUrl(url);
            creature.setImageUrl(fImageUrl);
            creature.setHp(fHp);
            creature.setExperience(fExperience);
            creature.setPhysicalResistance(fPhysical);
            creature.setFireResistance(fFire);
            creature.setIceResistance(fIce);
            creature.setEnergyResistance(fEnergy);
            creature.setEarthResistance(fEarth);
            creature.setDeathResistance(fDeath);
            creature.setHolyResistance(fHoly);
            creature.setDrownResistance(fDrown);
            Creature saved = creatureRepository.save(creature);

            for (ParsedLoot loot : lootEntries) {
                Item item = itemRepository.findByName(loot.itemName()).orElse(null);
                if (item == null) {
                    log.warn("Unknown loot item '{}' for creature '{}' — skipping", loot.itemName(), fName);
                    continue;
                }
                CreatureLoot cl = creatureLootRepository
                        .findByCreatureIdAndItemId(saved.getId(), item.getId())
                        .orElseGet(CreatureLoot::new);
                cl.setCreature(saved);
                cl.setItem(item);
                cl.setRarity(loot.rarity());
                cl.setMinAmount(loot.minAmount());
                cl.setMaxAmount(loot.maxAmount());
                creatureLootRepository.save(cl);
            }
        });

        return true;
    }

    private boolean isCreaturePage(Element infobox) {
        for (Element row : infobox.select("tr")) {
            Elements cells = row.select("td");
            if (cells.isEmpty()) continue;
            String label = cells.get(0).text().trim();
            if (label.equals("Hit Points") || label.equals("Experience Points")) return true;
        }
        return false;
    }

    private String getInfoboxValue(Element infobox, String label) {
        for (Element row : infobox.select("tr")) {
            Elements cells = row.select("td");
            if (cells.size() < 2) continue;
            if (cells.get(0).text().trim().equals(label)) return cells.get(1).text().trim();
        }
        return null;
    }

    private Integer parseInfoboxInt(Element infobox, String label, String url) {
        String value = getInfoboxValue(infobox, label);
        if (value == null) return null;
        try {
            return Integer.parseInt(value.replace(",", "").trim());
        } catch (NumberFormatException e) {
            log.warn("Could not parse '{}' value '{}' on {}", label, value, url);
            return null;
        }
    }

    private Integer parseResistance(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Integer.parseInt(value.replace("%", "").replace("+", "").trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private List<ParsedLoot> parseLootTable(Document doc) {
        List<ParsedLoot> entries = new ArrayList<>();
        for (Element table : doc.select("table")) {
            for (Element row : table.select("tr")) {
                Elements cells = row.select("td");
                if (cells.isEmpty()) continue;
                // Loot rows have a link to an item in the first cell
                Elements links = cells.get(0).select("a[href]");
                if (links.isEmpty()) continue;
                String itemName = links.get(0).text().trim();
                if (itemName.isBlank()) continue;

                String rarity = extractRarity(row.className().toLowerCase());

                int minAmount = 1;
                int maxAmount = 1;
                if (cells.size() > 1) {
                    int[] range = parseAmountRange(cells.get(1).text().trim());
                    minAmount = range[0];
                    maxAmount = range[1];
                }
                entries.add(new ParsedLoot(itemName, rarity, minAmount, maxAmount));
            }
        }
        return entries;
    }

    private String extractRarity(String cssClass) {
        if (cssClass.contains("very-rare") || cssClass.contains("very_rare")) return "very rare";
        if (cssClass.contains("semi-rare") || cssClass.contains("semi_rare")) return "semi-rare";
        if (cssClass.contains("uncommon")) return "uncommon";
        // Check "rare" after the compound forms above to avoid false matches
        if (cssClass.contains("rare")) return "rare";
        if (cssClass.contains("common")) return "common";
        return null;
    }

    private int[] parseAmountRange(String text) {
        if (text == null || text.isBlank()) return new int[]{1, 1};
        Matcher range = Pattern.compile("(\\d+)-(\\d+)").matcher(text);
        if (range.find()) {
            try {
                return new int[]{Integer.parseInt(range.group(1)), Integer.parseInt(range.group(2))};
            } catch (NumberFormatException e) {
                return new int[]{1, 1};
            }
        }
        Matcher single = Pattern.compile("\\d+").matcher(text);
        if (single.find()) {
            try {
                int val = Integer.parseInt(single.group());
                return new int[]{val, val};
            } catch (NumberFormatException e) {
                return new int[]{1, 1};
            }
        }
        return new int[]{1, 1};
    }

    // Only accepts relative paths or absolute URLs already on tibia.fandom.com — prevents SSRF
    private String safeUrl(String href) {
        if (href == null || href.isBlank()) return null;
        if (href.startsWith("/")) return BASE_URL + href;
        if (href.startsWith(BASE_URL)) return href;
        log.warn("Rejected potentially unsafe href: {}", href);
        return null;
    }

    private record ParsedLoot(String itemName, String rarity, int minAmount, int maxAmount) {}
}
