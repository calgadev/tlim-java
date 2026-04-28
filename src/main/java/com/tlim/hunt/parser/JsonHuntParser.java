package com.tlim.hunt.parser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class JsonHuntParser {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd, HH:mm:ss");

    // Anchored at start, fixed alternatives only — no quantified groups, no backtracking risk
    private static final Pattern ARTICLE_PATTERN = Pattern.compile("^(a|an) ");

    private final ObjectMapper objectMapper;

    public JsonHuntParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ParsedHunt parse(String rawJson) {
        JsonNode root;
        try {
            root = objectMapper.readTree(rawJson);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Malformed JSON: " + e.getOriginalMessage(), e);
        }

        try {
            LocalDateTime sessionStart = LocalDateTime.parse(root.get("Session start").asText(), DATE_FMT);
            LocalDateTime sessionEnd = LocalDateTime.parse(root.get("Session end").asText(), DATE_FMT);
            String duration = root.get("Session length").asText();
            int rawXp = parseIntNode(root.get("Raw XP Gain"));
            int xpWithBonus = parseIntNode(root.get("XP Gain"));
            int lootTotal = parseIntNode(root.get("Loot"));
            int supplies = parseIntNode(root.get("Supplies"));
            int damage = parseIntNode(root.get("Damage"));
            int healing = parseIntNode(root.get("Healing"));

            List<ParsedItem> items = new ArrayList<>();
            JsonNode lootedItems = root.get("Looted Items");
            if (lootedItems != null && lootedItems.isArray()) {
                for (JsonNode entry : lootedItems) {
                    String name = ARTICLE_PATTERN.matcher(entry.get("Name").asText()).replaceFirst("");
                    int count = entry.get("Count").asInt();
                    items.add(new ParsedItem(name, count));
                }
            }

            List<ParsedMonster> monsters = new ArrayList<>();
            JsonNode killedMonsters = root.get("Killed Monsters");
            if (killedMonsters != null && killedMonsters.isArray()) {
                for (JsonNode entry : killedMonsters) {
                    String name = entry.get("Name").asText();
                    int count = entry.get("Count").asInt();
                    monsters.add(new ParsedMonster(name, count));
                }
            }

            return new ParsedHunt(sessionStart, sessionEnd, duration,
                    rawXp, xpWithBonus, lootTotal, supplies, damage, healing,
                    items, monsters);

        } catch (NullPointerException | java.time.format.DateTimeParseException | NumberFormatException e) {
            throw new IllegalArgumentException("Invalid or missing field in JSON hunt data: " + e.getMessage(), e);
        }
    }

    private int parseIntNode(JsonNode node) {
        return Integer.parseInt(node.asText().replace(",", ""));
    }
}
