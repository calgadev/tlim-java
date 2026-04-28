package com.tlim.hunt.parser;

import java.time.LocalDateTime;
import java.util.List;

public record ParsedHunt(
        LocalDateTime sessionStart,
        LocalDateTime sessionEnd,
        String duration,
        int rawXp,
        int xpWithBonus,
        int lootTotal,
        int supplies,
        int damage,
        int healing,
        List<ParsedItem> items,
        List<ParsedMonster> monsters
) {}
