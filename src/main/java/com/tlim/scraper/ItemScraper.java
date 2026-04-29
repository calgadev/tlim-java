package com.tlim.scraper;

import com.fasterxml.jackson.databind.JsonNode;
import com.tlim.item.Item;
import com.tlim.item.ItemRepository;
import com.tlim.item.NpcBuyer;
import com.tlim.item.NpcBuyerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class ItemScraper {

    private static final Logger log = LoggerFactory.getLogger(ItemScraper.class);

    private final WikiApiClient wikiApiClient;
    private final ItemRepository itemRepository;
    private final NpcBuyerRepository npcBuyerRepository;
    private final TransactionTemplate transactionTemplate;

    public ItemScraper(WikiApiClient wikiApiClient, ItemRepository itemRepository,
                       NpcBuyerRepository npcBuyerRepository, TransactionTemplate transactionTemplate) {
        this.wikiApiClient = wikiApiClient;
        this.itemRepository = itemRepository;
        this.npcBuyerRepository = npcBuyerRepository;
        this.transactionTemplate = transactionTemplate;
    }

    public int[] scrapeAll() {
        int scraped = 0;
        int failed = 0;

        // Level 1: subcategories of Category:Item_Types
        List<String> categories = fetchCategoryMembers("Category:Items", "subcat");
        log.info("Found {} item categories", categories.size());

        // Level 2: page titles within each category, deduplicated
        LinkedHashSet<String> itemTitles = new LinkedHashSet<>();
        for (String category : categories) {
            itemTitles.addAll(fetchCategoryMembers(category, "page"));
        }
        log.info("Found {} unique item pages", itemTitles.size());

        // Level 3: parse and upsert each item
        for (String title : itemTitles) {
            try {
                scrapeItem(title);
                scraped++;
            } catch (Exception e) {
                log.warn("Failed to scrape item '{}': {}", title, e.getMessage());
                failed++;
            }
        }

        return new int[]{scraped, failed};
    }

    private void scrapeItem(String pageTitle) {
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

        String infoboxBlock = extractBlock(wikitext, "{{Infobox Object");
        if (infoboxBlock == null) {
            log.debug("No Infobox Object found for page '{}'", pageTitle);
            return;
        }

        Map<String, String> fields = parseFields(infoboxBlock);

        String name = fields.getOrDefault("name", pageTitle).trim();
        if (name.isEmpty()) name = pageTitle;

        BigDecimal weight = parseWeight(fields.get("weight"));

        String category = fields.containsKey("primarytype")
                ? nullIfBlank(fields.get("primarytype"))
                : nullIfBlank(fields.get("objectclass"));

        String description = nullIfBlank(fields.get("notes"));

        String imageFilename = fields.containsKey("image")
                ? nullIfBlank(fields.get("image"))
                : nullIfBlank(fields.get("sprite"));
        // Infobox Object has no image/sprite field — derive from item name (TibiaWiki convention)
        String imageUrl = imageFilename != null
                ? "https://tibia.fandom.com/wiki/Special:FilePath/" + imageFilename.replace(" ", "_")
                : "https://tibia.fandom.com/wiki/Special:FilePath/" + name.replace(" ", "_") + ".gif";

        String wikiUrl = "https://tibia.fandom.com/wiki/" + pageTitle.replace(" ", "_");

        boolean isQuestItem = "yes".equalsIgnoreCase(fields.get("questitem"));
        boolean isImbuementMaterial = "yes".equalsIgnoreCase(fields.get("imbuing"));

        // Upsert item
        Item item = itemRepository.findByName(name).orElse(new Item());
        item.setName(name);
        item.setWikiUrl(wikiUrl);
        item.setDescription(description);
        item.setImageUrl(imageUrl);
        item.setWeight(weight);
        item.setCategory(category);
        item.setQuestItem(isQuestItem);
        item.setImbuementMaterial(isImbuementMaterial);
        item.setDeliveryItem(false);
        item = itemRepository.save(item);

        // Parse NPC buyers from wikitext before touching the database
        List<ParsedBuyer> parsedBuyers = new ArrayList<>();
        String npcBlock = extractBlock(wikitext, "{{NPC Buyers");
        if (npcBlock != null) {
            // NPC buyer keys are NPC names — parse without lowercasing the key
            String npcInner = npcBlock.substring(2, npcBlock.length() - 2);
            List<String> npcSegments = splitAtTopLevelPipe(npcInner);
            for (int i = 1; i < npcSegments.size(); i++) {
                String segment = npcSegments.get(i).trim();
                int eq = segment.indexOf('=');
                if (eq == -1) continue;
                String npcName = segment.substring(0, eq).trim();
                String priceStr = segment.substring(eq + 1).trim().replace(",", "");
                if (npcName.isEmpty() || priceStr.isEmpty()) continue;
                try {
                    parsedBuyers.add(new ParsedBuyer(npcName, Integer.parseInt(priceStr)));
                } catch (NumberFormatException e) {
                    // skip entries with unparseable prices
                }
            }
        } else {
            log.debug("No NPC Buyers block found for item '{}'", name);
        }

        // Replace NPC buyers atomically: if an insert fails mid-way, the delete is rolled back too
        final Item savedItem = item;
        transactionTemplate.executeWithoutResult(status -> {
            npcBuyerRepository.deleteAllByItemId(savedItem.getId());
            for (ParsedBuyer parsed : parsedBuyers) {
                NpcBuyer buyer = new NpcBuyer();
                buyer.setItem(savedItem);
                buyer.setNpcName(parsed.npcName());
                buyer.setLocation(null);
                buyer.setPrice(parsed.price());
                npcBuyerRepository.save(buyer);
            }
        });
    }

    private record ParsedBuyer(String npcName, int price) {}

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
     * Finds a template block starting with blockStart, matching nested {{ }}.
     */
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

    /**
     * Splits a template block at top-level | characters and returns key=value pairs
     * with keys lowercased (for case-insensitive infobox field lookup).
     */
    private Map<String, String> parseFields(String block) {
        Map<String, String> fields = new LinkedHashMap<>();
        // Strip outer {{ and }} so pipes inside the block are seen at depth 0
        String inner = block.substring(2, block.length() - 2);
        List<String> segments = splitAtTopLevelPipe(inner);
        // First segment is the template name — skip it
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

    private BigDecimal parseWeight(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String cleaned = raw.replaceAll("[^0-9.]", "");
        if (cleaned.isEmpty()) return null;
        try {
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String nullIfBlank(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}
