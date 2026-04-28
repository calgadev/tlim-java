package com.tlim.hunt;

import com.tlim.character.Character;
import com.tlim.character.CharacterRepository;
import com.tlim.creature.Creature;
import com.tlim.creature.CreatureRepository;
import com.tlim.hunt.dto.*;
import com.tlim.hunt.parser.*;
import com.tlim.item.Item;
import com.tlim.item.ItemRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class HuntImportService {

    private static final Logger log = LoggerFactory.getLogger(HuntImportService.class);
    private static final int LOG_NAME_MAX_LENGTH = 100;

    private final TextHuntParser textHuntParser;
    private final JsonHuntParser jsonHuntParser;
    private final HuntSessionRepository huntSessionRepository;
    private final HuntSessionItemRepository huntSessionItemRepository;
    private final HuntSessionMonsterRepository huntSessionMonsterRepository;
    private final ItemRepository itemRepository;
    private final CreatureRepository creatureRepository;
    private final CharacterRepository characterRepository;

    public HuntImportService(TextHuntParser textHuntParser,
                             JsonHuntParser jsonHuntParser,
                             HuntSessionRepository huntSessionRepository,
                             HuntSessionItemRepository huntSessionItemRepository,
                             HuntSessionMonsterRepository huntSessionMonsterRepository,
                             ItemRepository itemRepository,
                             CreatureRepository creatureRepository,
                             CharacterRepository characterRepository) {
        this.textHuntParser = textHuntParser;
        this.jsonHuntParser = jsonHuntParser;
        this.huntSessionRepository = huntSessionRepository;
        this.huntSessionItemRepository = huntSessionItemRepository;
        this.huntSessionMonsterRepository = huntSessionMonsterRepository;
        this.itemRepository = itemRepository;
        this.creatureRepository = creatureRepository;
        this.characterRepository = characterRepository;
    }

    @Transactional
    public HuntSessionResponse importFromText(HuntImportRequest req) {
        ParsedHunt parsed = textHuntParser.parse(req.rawData());
        return persistParsedHunt(parsed, req);
    }

    @Transactional
    public HuntSessionResponse importFromJson(HuntImportRequest req) {
        ParsedHunt parsed = jsonHuntParser.parse(req.rawData());
        return persistParsedHunt(parsed, req);
    }

    private HuntSessionResponse persistParsedHunt(ParsedHunt parsed, HuntImportRequest req) {
        Character character = characterRepository.findById(req.characterId())
                .orElseThrow(() -> new EntityNotFoundException("Character not found: " + req.characterId()));

        HuntSession session = new HuntSession();
        session.setCharacter(character);
        session.setStartedAt(parsed.sessionStart().toInstant(ZoneOffset.UTC));
        session.setEndedAt(parsed.sessionEnd().toInstant(ZoneOffset.UTC));
        session.setDuration(parsed.duration());
        session.setRawXp(parsed.rawXp());
        session.setXpWithBonus(parsed.xpWithBonus());
        session.setLootTotal(parsed.lootTotal());
        session.setSupplies(parsed.supplies());
        session.setDamage(parsed.damage());
        session.setHealing(parsed.healing());
        session.setLocation(req.location());
        session.setParty(req.isParty() != null && req.isParty());
        session.setNotes(req.notes());
        session.setCharLevel(req.charLevel());
        session.setAllyEkLevel(req.allyEkLevel());
        session.setAllyMsLevel(req.allyMsLevel());
        session.setAllyEdLevel(req.allyEdLevel());
        session.setAllyRpLevel(req.allyRpLevel());
        session.setAllyEmLevel(req.allyEmLevel());

        String sessionName = (req.name() == null || req.name().isBlank())
                ? "Hunt " + parsed.sessionStart().format(DateTimeFormatter.ofPattern("dd/MM HH:mm"))
                : req.name();
        session.setName(sessionName);

        huntSessionRepository.save(session);

        List<HuntSessionItemResponse> savedItems = new ArrayList<>();
        List<String> skippedItems = new ArrayList<>();

        for (ParsedItem parsedItem : parsed.items()) {
            Optional<Item> itemOpt = itemRepository.findByName(parsedItem.name());
            if (itemOpt.isEmpty()) {
                log.warn("Unknown item '{}', skipping", truncate(parsedItem.name()));
                skippedItems.add(parsedItem.name());
                continue;
            }
            Item item = itemOpt.get();
            HuntSessionItem sessionItem = new HuntSessionItem();
            sessionItem.setHuntSession(session);
            sessionItem.setItem(item);
            sessionItem.setQuantity(parsedItem.quantity());
            HuntSessionItem saved = huntSessionItemRepository.save(sessionItem);
            savedItems.add(new HuntSessionItemResponse(saved.getId(), item.getId(), item.getName(), parsedItem.quantity()));
        }

        List<HuntSessionMonsterResponse> savedMonsters = new ArrayList<>();
        List<String> skippedMonsters = new ArrayList<>();

        for (ParsedMonster parsedMonster : parsed.monsters()) {
            Optional<Creature> creatureOpt = creatureRepository.findByName(parsedMonster.name());
            if (creatureOpt.isEmpty()) {
                log.warn("Unknown creature '{}', skipping", truncate(parsedMonster.name()));
                skippedMonsters.add(parsedMonster.name());
                continue;
            }
            Creature creature = creatureOpt.get();
            HuntSessionMonster sessionMonster = new HuntSessionMonster();
            sessionMonster.setHuntSession(session);
            sessionMonster.setCreature(creature);
            sessionMonster.setKillCount(parsedMonster.quantity());
            HuntSessionMonster saved = huntSessionMonsterRepository.save(sessionMonster);
            savedMonsters.add(new HuntSessionMonsterResponse(saved.getId(), creature.getId(), creature.getName(), parsedMonster.quantity()));
        }

        return new HuntSessionResponse(
                session.getId(),
                character.getId(),
                session.getName(),
                session.getLocation(),
                session.isParty(),
                session.getNotes(),
                session.getStartedAt(),
                session.getEndedAt(),
                session.getDuration(),
                session.getRawXp(),
                session.getXpWithBonus(),
                session.getLootTotal(),
                session.getSupplies(),
                session.getDamage(),
                session.getHealing(),
                session.getCharLevel(),
                session.getAllyEkLevel(),
                session.getAllyMsLevel(),
                session.getAllyEdLevel(),
                session.getAllyRpLevel(),
                session.getAllyEmLevel(),
                savedItems,
                savedMonsters,
                skippedItems,
                skippedMonsters
        );
    }

    // Truncates user-supplied names before logging to prevent log flooding
    private static String truncate(String name) {
        if (name == null) return "(null)";
        return name.length() > LOG_NAME_MAX_LENGTH
                ? name.substring(0, LOG_NAME_MAX_LENGTH) + "..."
                : name;
    }
}
