package com.tlim.hunt;

import com.tlim.character.Character;
import com.tlim.character.CharacterRepository;
import com.tlim.creature.Creature;
import com.tlim.creature.CreatureRepository;
import com.tlim.hunt.dto.*;
import com.tlim.hunt.parser.*;
import com.tlim.inventory.Inventory;
import com.tlim.inventory.InventoryRepository;
import com.tlim.item.Item;
import com.tlim.item.ItemRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private final InventoryRepository inventoryRepository;

    public HuntImportService(TextHuntParser textHuntParser,
                             JsonHuntParser jsonHuntParser,
                             HuntSessionRepository huntSessionRepository,
                             HuntSessionItemRepository huntSessionItemRepository,
                             HuntSessionMonsterRepository huntSessionMonsterRepository,
                             ItemRepository itemRepository,
                             CreatureRepository creatureRepository,
                             CharacterRepository characterRepository,
                             InventoryRepository inventoryRepository) {
        this.textHuntParser = textHuntParser;
        this.jsonHuntParser = jsonHuntParser;
        this.huntSessionRepository = huntSessionRepository;
        this.huntSessionItemRepository = huntSessionItemRepository;
        this.huntSessionMonsterRepository = huntSessionMonsterRepository;
        this.itemRepository = itemRepository;
        this.creatureRepository = creatureRepository;
        this.characterRepository = characterRepository;
        this.inventoryRepository = inventoryRepository;
    }

    @Transactional
    public HuntSessionResponse importFromText(HuntImportRequest req, Long currentUserId) {
        ParsedHunt parsed = textHuntParser.parse(req.rawData());
        return persistParsedHunt(parsed, req, currentUserId);
    }

    @Transactional
    public HuntSessionResponse importFromJson(HuntImportRequest req, Long currentUserId) {
        ParsedHunt parsed = jsonHuntParser.parse(req.rawData());
        return persistParsedHunt(parsed, req, currentUserId);
    }

    private HuntSessionResponse persistParsedHunt(ParsedHunt parsed, HuntImportRequest req, Long currentUserId) {
        Character character = characterRepository.findById(req.characterId())
                .orElseThrow(() -> new EntityNotFoundException("Character not found: " + req.characterId()));
        if (!character.getUser().getId().equals(currentUserId)) {
            throw new AccessDeniedException("Character does not belong to the authenticated user");
        }

        // Resolve names first. Unknown names are reported back instead of failing the import,
        // and entries that resolve to the same record are merged (unique per session).
        List<String> skippedItems = new ArrayList<>();
        Map<Long, Item> itemsById = new LinkedHashMap<>();
        Map<Long, Integer> itemQuantities = new LinkedHashMap<>();
        for (ParsedItem parsedItem : parsed.items()) {
            Optional<Item> itemOpt = findItem(parsedItem.name());
            if (itemOpt.isEmpty()) {
                log.warn("Unknown item '{}', skipping", truncate(parsedItem.name()));
                skippedItems.add(parsedItem.name());
                continue;
            }
            Item item = itemOpt.get();
            itemsById.put(item.getId(), item);
            itemQuantities.merge(item.getId(), parsedItem.quantity(), Integer::sum);
        }

        List<String> skippedMonsters = new ArrayList<>();
        Map<Long, Creature> creaturesById = new LinkedHashMap<>();
        Map<Long, Integer> killCounts = new LinkedHashMap<>();
        for (ParsedMonster parsedMonster : parsed.monsters()) {
            Optional<Creature> creatureOpt = findCreature(parsedMonster.name());
            if (creatureOpt.isEmpty()) {
                log.warn("Unknown creature '{}', skipping", truncate(parsedMonster.name()));
                skippedMonsters.add(parsedMonster.name());
                continue;
            }
            Creature creature = creatureOpt.get();
            creaturesById.put(creature.getId(), creature);
            killCounts.merge(creature.getId(), parsedMonster.quantity(), Integer::sum);
        }

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
        session.setLocation(blankToNull(req.location()));
        session.setParty(req.isParty() != null && req.isParty());
        session.setNotes(blankToNull(req.notes()));
        session.setCharLevel(req.charLevel());
        session.setAllyEkLevel(req.allyEkLevel());
        session.setAllyMsLevel(req.allyMsLevel());
        session.setAllyEdLevel(req.allyEdLevel());
        session.setAllyRpLevel(req.allyRpLevel());
        session.setAllyEmLevel(req.allyEmLevel());

        String sessionName = (req.name() == null || req.name().isBlank())
                ? "Hunt " + parsed.sessionStart().format(DateTimeFormatter.ofPattern("dd/MM HH:mm"))
                : req.name().trim();
        session.setName(sessionName);

        huntSessionRepository.save(session);

        List<HuntSessionItemResponse> savedItems = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : itemQuantities.entrySet()) {
            Item item = itemsById.get(entry.getKey());
            int quantity = entry.getValue();

            HuntSessionItem sessionItem = new HuntSessionItem();
            sessionItem.setHuntSession(session);
            sessionItem.setItem(item);
            sessionItem.setQuantity(quantity);
            HuntSessionItem saved = huntSessionItemRepository.save(sessionItem);
            savedItems.add(new HuntSessionItemResponse(saved.getId(), item.getId(), item.getName(), quantity));

            addToInventory(character, item, quantity);
        }

        List<HuntSessionMonsterResponse> savedMonsters = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : killCounts.entrySet()) {
            Creature creature = creaturesById.get(entry.getKey());
            int count = entry.getValue();

            HuntSessionMonster sessionMonster = new HuntSessionMonster();
            sessionMonster.setHuntSession(session);
            sessionMonster.setCreature(creature);
            sessionMonster.setKillCount(count);
            HuntSessionMonster saved = huntSessionMonsterRepository.save(sessionMonster);
            savedMonsters.add(new HuntSessionMonsterResponse(saved.getId(), creature.getId(), creature.getName(), count));
        }

        return HuntSessionService.buildResponse(session, savedItems, savedMonsters, skippedItems, skippedMonsters);
    }

    // Looted items go straight into the character's stock, as in Stage 1.
    // New entries start with no goal (targetQuantity = 0), so everything is available for sale.
    private void addToInventory(Character character, Item item, int quantity) {
        Inventory inventory = inventoryRepository
                .findByCharacterIdAndItemId(character.getId(), item.getId())
                .orElseGet(() -> {
                    Inventory created = new Inventory();
                    created.setCharacter(character);
                    created.setItem(item);
                    created.setCurrentQuantity(0);
                    created.setTargetQuantity(0);
                    return created;
                });
        inventory.setCurrentQuantity(inventory.getCurrentQuantity() + quantity);
        inventoryRepository.save(inventory);
    }

    // Hunt Analyser writes in-game lowercase names ("gold coin"), which is TibiaWiki's "actualname".
    // The display name ("Gold Coin") is the fallback for manually created records.
    private Optional<Item> findItem(String name) {
        Optional<Item> byActualName = itemRepository.findFirstByActualNameIgnoreCase(name);
        return byActualName.isPresent() ? byActualName : itemRepository.findFirstByNameIgnoreCase(name);
    }

    private Optional<Creature> findCreature(String name) {
        Optional<Creature> byActualName = creatureRepository.findFirstByActualNameIgnoreCase(name);
        return byActualName.isPresent() ? byActualName : creatureRepository.findFirstByNameIgnoreCase(name);
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    // Truncates user-supplied names before logging to prevent log flooding
    private static String truncate(String name) {
        if (name == null) return "(null)";
        return name.length() > LOG_NAME_MAX_LENGTH
                ? name.substring(0, LOG_NAME_MAX_LENGTH) + "..."
                : name;
    }
}
