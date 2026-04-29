package com.tlim.scraper;

import com.fasterxml.jackson.databind.JsonNode;
import com.tlim.creature.Creature;
import com.tlim.creature.CreatureLoot;
import com.tlim.creature.CreatureLootRepository;
import com.tlim.creature.CreatureRepository;
import com.tlim.item.Item;
import com.tlim.item.ItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class CreatureScraper {

    private static final Logger log = LoggerFactory.getLogger(CreatureScraper.class);

    private final WikiApiClient wikiApiClient;
    private final CreatureRepository creatureRepository;
    private final CreatureLootRepository creatureLootRepository;
    private final ItemRepository itemRepository;

    public CreatureScraper(WikiApiClient wikiApiClient, CreatureRepository creatureRepository,
                           CreatureLootRepository creatureLootRepository, ItemRepository itemRepository) {
        this.wikiApiClient = wikiApiClient;
        this.creatureRepository = creatureRepository;
        this.creatureLootRepository = creatureLootRepository;
        this.itemRepository = itemRepository;
    }

    public int[] scrapeAll() {
        int scraped = 0;
        int failed = 0;

        // Level 1: subcategories of Category:Creatures
        List<String> categories = fetchCategoryMembers("Category:Creatures", "subcat");
        log.info("Found {} creature categories", categories.size());

        // Level 2: page titles within each category, deduplicated
        LinkedHashSet<String> creatureTitles = new LinkedHashSet<>();
        for (String category : categories) {
            creatureTitles.addAll(fetchCategoryMembers(category, "page"));
        }
        log.info("Found {} unique creature pages", creatureTitles.size());

        // Level 3: parse and upsert each creature
        for (String title : creatureTitles) {
            try {
                scrapeCreature(title);
                scraped++;
            } catch (Exception e) {
                log.warn("Failed to scrape creature '{}': {}", title, e.getMessage());
                failed++;
            }
        }

        return new int[]{scraped, failed};
    }

    private void scrapeCreature(String pageTitle) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("action", "parse");
        params.put("page", pageTitle);
        params.put("prop", "wikitext");
        Optional<JsonNode> result = wikiApiClient.get(params);
        if (result.isEmpty()) {
            log.warn("Skipping page '{}' due to HTTP error — will be missing from this run", pageTitle);
            return;
        }
        JsonNode root = result.get();

        String wikitext = root.path("parse").path("wikitext").asText("");

        // Skip pages that are not creature pages
        if (!wikitext.toLowerCase().contains("{{infobox creature")) {
            log.debug("No Infobox Creature found for page '{}' — skipping", pageTitle);
            return;
        }

        String infoboxBlock = extractBlock(wikitext, "{{Infobox Creature");
        if (infoboxBlock == null) {
            log.debug("Could not extract Infobox Creature block for page '{}' — skipping", pageTitle);
            return;
        }

        Map<String, String> fields = parseFields(infoboxBlock);

        String name = fields.getOrDefault("name", pageTitle).trim();
        if (name.isEmpty()) name = pageTitle;

        String imageFilename = fields.containsKey("image")
                ? nullIfBlank(fields.get("image"))
                : nullIfBlank(fields.get("sprite"));
        String imageUrl = imageFilename != null
                ? "https://tibia.fandom.com/wiki/Special:FilePath/" + imageFilename.replace(" ", "_")
                : null;

        String wikiUrl = "https://tibia.fandom.com/wiki/" + pageTitle.replace(" ", "_");

        Integer hp = parseNullableInt(fields.get("hp"));
        Integer experience = parseNullableInt(fields.get("exp"));

        Integer physicalResistance = parseResistance(fields.get("physicaldmgmod"));
        Integer fireResistance     = parseResistance(fields.get("firedmgmod"));
        Integer iceResistance      = parseResistance(fields.get("icedmgmod"));
        Integer energyResistance   = parseResistance(fields.get("energydmgmod"));
        Integer earthResistance    = parseResistance(fields.get("earthdmgmod"));
        Integer deathResistance    = parseResistance(fields.get("deathdmgmod"));
        Integer holyResistance     = parseResistance(fields.get("holydmgmod"));
        Integer drownResistance    = parseResistance(fields.get("drowndmgmod"));

        // Upsert creature
        Creature creature = creatureRepository.findByName(name).orElse(new Creature());
        creature.setName(name);
        creature.setWikiUrl(wikiUrl);
        creature.setImageUrl(imageUrl);
        creature.setHp(hp);
        creature.setExperience(experience);
        creature.setPhysicalResistance(physicalResistance);
        creature.setFireResistance(fireResistance);
        creature.setIceResistance(iceResistance);
        creature.setEnergyResistance(energyResistance);
        creature.setEarthResistance(earthResistance);
        creature.setDeathResistance(deathResistance);
        creature.setHolyResistance(holyResistance);
        creature.setDrownResistance(drownResistance);
        creature = creatureRepository.save(creature);

        // Parse and upsert loot entries
        String lootBlock = findLootBlock(wikitext);
        if (lootBlock != null) {
            for (LootEntry entry : parseLootEntries(lootBlock)) {
                Item item = itemRepository.findByName(entry.itemName()).orElse(null);
                if (item == null) {
                    log.warn("Unknown loot item '{}' for creature '{}' — skipping", entry.itemName(), name);
                    continue;
                }
                CreatureLoot loot = creatureLootRepository
                        .findByCreatureIdAndItemId(creature.getId(), item.getId())
                        .orElse(new CreatureLoot());
                loot.setCreature(creature);
                loot.setItem(item);
                loot.setRarity(entry.rarity());
                loot.setMinAmount(entry.minAmount());
                loot.setMaxAmount(entry.maxAmount());
                creatureLootRepository.save(loot);
            }
        }
    }

    private List<String> fetchCategoryMembers(String cmtitle, String cmtype) {
        List<String> titles = new ArrayList<>();
        String continueToken = null;
        do {
            Map<String, String> params = new LinkedHashMap<>();
            params.put("action", "query");
            params.put("list", "categorymembers");
            params.put("cmtitle", cmtitle);
            params.put("cmtype", cmtype);
            params.put("cmlimit", "500");
            if (continueToken != null) {
                params.put("cmcontinue", continueToken);
            }
            Optional<JsonNode> result = wikiApiClient.get(params);
            if (result.isEmpty()) {
                log.warn("HTTP error fetching category members for '{}' — results may be incomplete", cmtitle);
                break;
            }
            JsonNode root = result.get();
            for (JsonNode member : root.path("query").path("categorymembers")) {
                titles.add(member.path("title").asText());
            }
            JsonNode next = root.path("continue").path("cmcontinue");
            continueToken = (next.isMissingNode() || next.isNull()) ? null : next.asText();
        } while (continueToken != null);
        return titles;
    }

    /**
     * Finds {{Loot2}} first; falls back to {{Loot}} (not followed by an alphanumeric char,
     * to avoid matching {{Loot2}} when searching for {{Loot}}).
     */
    private String findLootBlock(String wikitext) {
        String lower = wikitext.toLowerCase();
        int loot2Idx = lower.indexOf("{{loot2");
        if (loot2Idx != -1) return extractBlockFrom(wikitext, loot2Idx);

        int pos = 0;
        while (pos < lower.length()) {
            int idx = lower.indexOf("{{loot", pos);
            if (idx == -1) return null;
            int afterLoot = idx + 6; // length of "{{loot"
            if (afterLoot >= lower.length() || !Character.isLetterOrDigit(lower.charAt(afterLoot))) {
                return extractBlockFrom(wikitext, idx);
            }
            pos = idx + 1;
        }
        return null;
    }

    private List<LootEntry> parseLootEntries(String lootBlock) {
        List<LootEntry> entries = new ArrayList<>();
        String lower = lootBlock.toLowerCase();
        int pos = 0;
        while (pos < lootBlock.length()) {
            int idx = lower.indexOf("{{loot item", pos);
            if (idx == -1) break;
            String lootItemBlock = extractBlockFrom(lootBlock, idx);
            if (lootItemBlock == null) break;
            // Strip outer {{ and }} to get "Loot Item|...|..."
            String inner = lootItemBlock.substring(2, lootItemBlock.length() - 2);
            String[] parts = inner.split("\\|", -1);
            // parts[0] = "Loot Item"
            // With amount:    parts[1]=amount, parts[2]=name, parts[3]=rarity
            // Without amount: parts[1]=name,   parts[2]=rarity
            if (parts.length >= 3) {
                String candidate = parts[1].trim();
                String amountStr;
                String itemName;
                String rarity;
                if (candidate.matches("\\d+(-\\d+)?")) {
                    amountStr = candidate;
                    itemName = parts[2].trim();
                    rarity = (parts.length >= 4 && !parts[3].trim().isEmpty()) ? parts[3].trim() : null;
                } else {
                    amountStr = null;
                    itemName = candidate;
                    rarity = !parts[2].trim().isEmpty() ? parts[2].trim() : null;
                }
                int[] range = parseAmountRange(amountStr);
                entries.add(new LootEntry(itemName, rarity, range[0], range[1]));
            }
            pos = idx + lootItemBlock.length();
        }
        return entries;
    }

    private String extractBlock(String wikitext, String blockStart) {
        int startIdx = wikitext.toLowerCase().indexOf(blockStart.toLowerCase());
        if (startIdx == -1) return null;
        return extractBlockFrom(wikitext, startIdx);
    }

    private String extractBlockFrom(String wikitext, int startIdx) {
        int depth = 0;
        int pos = startIdx;
        while (pos < wikitext.length() - 1) {
            if (wikitext.charAt(pos) == '{' && wikitext.charAt(pos + 1) == '{') {
                depth++;
                pos += 2;
            } else if (wikitext.charAt(pos) == '}' && wikitext.charAt(pos + 1) == '}') {
                depth--;
                pos += 2;
                if (depth == 0) return wikitext.substring(startIdx, pos);
            } else {
                pos++;
            }
        }
        return null;
    }

    private Map<String, String> parseFields(String block) {
        Map<String, String> fields = new LinkedHashMap<>();
        // Strip outer {{ and }} so pipes inside the block are seen at depth 0
        String inner = block.substring(2, block.length() - 2);
        List<String> segments = splitAtTopLevelPipe(inner);
        for (int i = 1; i < segments.size(); i++) {
            String segment = segments.get(i).trim();
            int eq = segment.indexOf('=');
            if (eq == -1) continue;
            String key = segment.substring(0, eq).trim().toLowerCase();
            String value = segment.substring(eq + 1).trim();
            if (!key.isEmpty()) {
                fields.put(key, value);
            }
        }
        return fields;
    }

    private List<String> splitAtTopLevelPipe(String text) {
        List<String> result = new ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i < text.length() - 1; i++) {
            char c = text.charAt(i);
            if (c == '{' && text.charAt(i + 1) == '{') {
                depth++;
                i++;
            } else if (c == '}' && text.charAt(i + 1) == '}') {
                depth--;
                i++;
            } else if (c == '|' && depth == 0) {
                result.add(text.substring(start, i));
                start = i + 1;
            }
        }
        result.add(text.substring(start));
        return result;
    }

    private Integer parseNullableInt(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Integer.parseInt(value.trim().replace(",", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer parseResistance(String value) {
        if (value == null || value.isBlank() || value.trim().equals("?")) return null;
        try {
            return Integer.parseInt(value.trim().replace(",", "").replace("%", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private int[] parseAmountRange(String amount) {
        if (amount == null || amount.isBlank() || amount.trim().equals("?")) return new int[]{1, 1};
        String trimmed = amount.trim();
        if (trimmed.contains("-")) {
            String[] parts = trimmed.split("-", 2);
            try {
                return new int[]{Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim())};
            } catch (NumberFormatException e) {
                return new int[]{1, 1};
            }
        }
        try {
            int val = Integer.parseInt(trimmed);
            return new int[]{val, val};
        } catch (NumberFormatException e) {
            return new int[]{1, 1};
        }
    }

    private String nullIfBlank(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private record LootEntry(String itemName, String rarity, int minAmount, int maxAmount) {}
}
