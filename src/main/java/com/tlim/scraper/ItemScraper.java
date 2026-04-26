package com.tlim.scraper;

import com.tlim.item.Item;
import com.tlim.item.ItemRepository;
import com.tlim.item.NpcBuyer;
import com.tlim.item.NpcBuyerRepository;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class ItemScraper {

    private static final Logger log = LoggerFactory.getLogger(ItemScraper.class);
    private static final String BASE_URL = "https://tibia.fandom.com";
    private static final String CATEGORY_INDEX_URL = BASE_URL + "/wiki/Category:Item_Types";

    private final WikiHttpClient wikiHttpClient;
    private final ItemRepository itemRepository;
    private final NpcBuyerRepository npcBuyerRepository;
    private final TransactionTemplate transactionTemplate;

    public ItemScraper(WikiHttpClient wikiHttpClient, ItemRepository itemRepository,
                       NpcBuyerRepository npcBuyerRepository, TransactionTemplate transactionTemplate) {
        this.wikiHttpClient = wikiHttpClient;
        this.itemRepository = itemRepository;
        this.npcBuyerRepository = npcBuyerRepository;
        this.transactionTemplate = transactionTemplate;
    }

    // Returns int[] { itemsScraped, itemsFailed }
    public int[] scrapeAll() {
        int scraped = 0;
        int failed = 0;

        // Level 1: collect category page URLs
        List<String> categoryUrls = collectCategoryUrls();
        log.info("Found {} item categories", categoryUrls.size());

        // Level 2: collect individual item page URLs from each category (with pagination)
        Set<String> itemUrls = new LinkedHashSet<>();
        for (String categoryUrl : categoryUrls) {
            itemUrls.addAll(collectItemUrls(categoryUrl));
        }
        log.info("Found {} unique item pages to scrape", itemUrls.size());

        // Level 3: scrape and upsert each item page
        for (String itemUrl : itemUrls) {
            try {
                scrapeAndUpsertItem(itemUrl);
                scraped++;
            } catch (Exception e) {
                log.warn("Failed to scrape item page {}: {}", itemUrl, e.getMessage());
                failed++;
            }
        }

        log.info("Item scrape complete: {} scraped, {} failed", scraped, failed);
        return new int[]{scraped, failed};
    }

    private List<String> collectCategoryUrls() {
        List<String> urls = new ArrayList<>();
        try {
            Document doc = wikiHttpClient.fetch(CATEGORY_INDEX_URL);
            for (Element link : doc.select(".category-page__member-link")) {
                String resolved = safeUrl(link.attr("href"));
                if (resolved != null) urls.add(resolved);
            }
        } catch (Exception e) {
            log.warn("Failed to collect category URLs from index: {}", e.getMessage());
        }
        return urls;
    }

    private List<String> collectItemUrls(String categoryUrl) {
        List<String> urls = new ArrayList<>();
        String currentUrl = categoryUrl;
        while (currentUrl != null) {
            try {
                Document doc = wikiHttpClient.fetch(currentUrl);
                for (Element link : doc.select(".category-page__member-link")) {
                    String resolved = safeUrl(link.attr("href"));
                    if (resolved != null) urls.add(resolved);
                }
                Element nextLink = doc.selectFirst("a.category-page__pagination-next");
                currentUrl = nextLink != null ? safeUrl(nextLink.attr("href")) : null;
            } catch (Exception e) {
                log.warn("Failed to collect item URLs from {}: {}", currentUrl, e.getMessage());
                currentUrl = null;
            }
        }
        return urls;
    }

    private void scrapeAndUpsertItem(String url) {
        Document doc = wikiHttpClient.fetch(url);

        Element titleEl = doc.selectFirst("h1.page-header__title");
        if (titleEl == null) throw new ScraperException("No page title found on: " + url);
        String name = titleEl.text().trim();
        if (name.isBlank()) throw new ScraperException("Blank page title on: " + url);

        Element infobox = doc.selectFirst(".infobox-wrapper");
        if (infobox == null) infobox = doc.selectFirst("table.infobox");

        String description = null;
        BigDecimal weight = null;
        String category = null;
        String imageUrl = null;
        boolean isQuestItem = false;
        boolean isImbuementMaterial = false;

        if (infobox != null) {
            Element img = infobox.selectFirst("img");
            if (img != null) {
                imageUrl = img.hasAttr("data-src") ? img.attr("data-src") : img.attr("src");
                if (imageUrl != null && imageUrl.isBlank()) imageUrl = null;
            }

            for (Element row : infobox.select("tr")) {
                Elements cells = row.select("td");
                if (cells.size() < 2) continue;
                String label = cells.get(0).text().trim();
                String value = cells.get(1).text().trim();

                switch (label) {
                    case "Notes", "Description" -> description = value.isEmpty() ? null : value;
                    case "Weight" -> {
                        try {
                            weight = new BigDecimal(value.replace(" oz", "").replace(",", "").trim());
                        } catch (NumberFormatException e) {
                            log.warn("Could not parse weight '{}' on {}", value, url);
                        }
                    }
                    case "Class" -> category = value.isEmpty() ? null : value;
                    case "Quest Item" -> isQuestItem = "Yes".equalsIgnoreCase(value);
                    case "Imbuing" -> isImbuementMaterial = "Yes".equalsIgnoreCase(value);
                }
            }
        }

        List<ParsedNpcBuyer> buyers = parseNpcBuyers(doc, url);

        // Capture finals for lambda
        final String fName = name;
        final String fDescription = description;
        final BigDecimal fWeight = weight;
        final String fCategory = category;
        final String fImageUrl = imageUrl;
        final boolean fIsQuestItem = isQuestItem;
        final boolean fIsImbuementMaterial = isImbuementMaterial;

        // Delete-then-insert for NPC buyers must be atomic — use TransactionTemplate
        transactionTemplate.executeWithoutResult(status -> {
            Item item = itemRepository.findByName(fName).orElseGet(Item::new);
            item.setName(fName);
            item.setWikiUrl(url);
            item.setDescription(fDescription);
            item.setWeight(fWeight);
            item.setCategory(fCategory);
            item.setImageUrl(fImageUrl);
            item.setQuestItem(fIsQuestItem);
            item.setImbuementMaterial(fIsImbuementMaterial);
            item.setDeliveryItem(false);
            Item saved = itemRepository.save(item);

            npcBuyerRepository.deleteAllInBatch(npcBuyerRepository.findByItemId(saved.getId()));

            for (ParsedNpcBuyer parsed : buyers) {
                NpcBuyer buyer = new NpcBuyer();
                buyer.setItem(saved);
                buyer.setNpcName(parsed.npcName());
                buyer.setLocation(parsed.location());
                buyer.setPrice(parsed.price());
                npcBuyerRepository.save(buyer);
            }
        });
    }

    private List<ParsedNpcBuyer> parseNpcBuyers(Document doc, String url) {
        List<ParsedNpcBuyer> buyers = new ArrayList<>();

        Element sellToEl = doc.getElementById("npc-trade-sellto");
        if (sellToEl == null) {
            log.debug("No NPC buyer section on: {}", url);
            return buyers;
        }

        // The trade table follows the section anchor as a sibling element
        Element table = sellToEl.nextElementSibling();
        while (table != null && !table.tagName().equals("table")) {
            table = table.nextElementSibling();
        }
        if (table == null) return buyers;

        for (Element row : table.select("tr")) {
            Elements cells = row.select("td");
            if (cells.size() < 3) continue;
            String npcName = cells.get(0).select("a").text().trim();
            if (npcName.isBlank()) continue;
            String location = cells.get(1).text().trim();
            // Strip non-numeric characters other than minus sign before parsing
            String priceText = cells.get(2).text().trim().replace(",", "").replaceAll("[^0-9\\-]", "").trim();
            try {
                int price = Integer.parseInt(priceText);
                buyers.add(new ParsedNpcBuyer(npcName, location.isEmpty() ? null : location, price));
            } catch (NumberFormatException e) {
                log.warn("Could not parse NPC buyer price '{}' for '{}' on {}", priceText, npcName, url);
            }
        }
        return buyers;
    }

    // Only accepts relative paths or absolute URLs already on tibia.fandom.com — prevents SSRF
    private String safeUrl(String href) {
        if (href == null || href.isBlank()) return null;
        if (href.startsWith("/")) return BASE_URL + href;
        if (href.startsWith(BASE_URL)) return href;
        log.warn("Rejected potentially unsafe href: {}", href);
        return null;
    }

    private record ParsedNpcBuyer(String npcName, String location, int price) {}
}
