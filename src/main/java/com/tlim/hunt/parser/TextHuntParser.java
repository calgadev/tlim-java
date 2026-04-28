package com.tlim.hunt.parser;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TextHuntParser {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd, HH:mm:ss");

    private static final Pattern SESSION_DATA_PATTERN = Pattern.compile(
            "From (\\d{4}-\\d{2}-\\d{2}, \\d{2}:\\d{2}:\\d{2}) to (\\d{4}-\\d{2}-\\d{2}, \\d{2}:\\d{2}:\\d{2})");

    // Anchored at start, fixed alternatives only — no quantified groups, no backtracking risk
    private static final Pattern COUNT_NAME_PATTERN = Pattern.compile("^(\\d+)x (.+)$");
    private static final Pattern ARTICLE_PATTERN = Pattern.compile("^(a|an) ");

    public ParsedHunt parse(String rawText) {
        LocalDateTime sessionStart = null;
        LocalDateTime sessionEnd = null;
        String duration = null;
        Integer rawXp = null;
        Integer xpWithBonus = null;
        Integer lootTotal = null;
        Integer supplies = null;
        Integer damage = null;
        Integer healing = null;

        List<ParsedItem> items = new ArrayList<>();
        List<ParsedMonster> monsters = new ArrayList<>();
        String section = null; // null = header, "monsters", "items"

        for (String rawLine : rawText.split("\n", -1)) {
            String line = rawLine.strip();
            if (line.isEmpty()) continue;

            if (line.equals("Killed Monsters:")) {
                section = "monsters";
                continue;
            }
            if (line.equals("Looted Items:")) {
                section = "items";
                continue;
            }

            if ("monsters".equals(section)) {
                Matcher m = COUNT_NAME_PATTERN.matcher(line);
                if (m.matches()) {
                    monsters.add(new ParsedMonster(m.group(2), Integer.parseInt(m.group(1))));
                }
                continue;
            }

            if ("items".equals(section)) {
                Matcher m = COUNT_NAME_PATTERN.matcher(line);
                if (m.matches()) {
                    String name = ARTICLE_PATTERN.matcher(m.group(2)).replaceFirst("");
                    items.add(new ParsedItem(name, Integer.parseInt(m.group(1))));
                }
                continue;
            }

            // Header section — parse known prefixes, silently ignore all others
            if (line.startsWith("Session data:")) {
                Matcher m = SESSION_DATA_PATTERN.matcher(line);
                if (m.find()) {
                    sessionStart = LocalDateTime.parse(m.group(1), DATE_FMT);
                    sessionEnd = LocalDateTime.parse(m.group(2), DATE_FMT);
                }
            } else if (line.startsWith("Session:")) {
                duration = line.substring("Session:".length()).trim();
            } else if (line.startsWith("Raw XP Gain:")) {
                rawXp = parseIntValue(line.substring("Raw XP Gain:".length()));
            } else if (line.startsWith("XP Gain:")) {
                xpWithBonus = parseIntValue(line.substring("XP Gain:".length()));
            } else if (line.startsWith("Loot:")) {
                lootTotal = parseIntValue(line.substring("Loot:".length()));
            } else if (line.startsWith("Supplies:")) {
                supplies = parseIntValue(line.substring("Supplies:".length()));
            } else if (line.startsWith("Damage:")) {
                damage = parseIntValue(line.substring("Damage:".length()));
            } else if (line.startsWith("Healing:")) {
                healing = parseIntValue(line.substring("Healing:".length()));
            }
        }

        validateRequiredFields(sessionStart, duration, rawXp, xpWithBonus, lootTotal, supplies, damage, healing);

        return new ParsedHunt(sessionStart, sessionEnd, duration,
                rawXp, xpWithBonus, lootTotal, supplies, damage, healing,
                items, monsters);
    }

    private Integer parseIntValue(String raw) {
        try {
            return Integer.parseInt(raw.trim().replace(",", ""));
        } catch (NumberFormatException e) {
            return null; // treated as missing; caught by validateRequiredFields
        }
    }

    private void validateRequiredFields(LocalDateTime sessionStart, String duration,
                                        Integer rawXp, Integer xpWithBonus, Integer lootTotal,
                                        Integer supplies, Integer damage, Integer healing) {
        List<String> missing = new ArrayList<>();
        if (sessionStart == null) missing.add("sessionStart");
        if (duration == null) missing.add("duration");
        if (rawXp == null) missing.add("rawXp");
        if (xpWithBonus == null) missing.add("xpWithBonus");
        if (lootTotal == null) missing.add("lootTotal");
        if (supplies == null) missing.add("supplies");
        if (damage == null) missing.add("damage");
        if (healing == null) missing.add("healing");
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("Missing fields: " + String.join(", ", missing));
        }
    }
}
