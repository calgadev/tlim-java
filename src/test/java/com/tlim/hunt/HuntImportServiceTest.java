package com.tlim.hunt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tlim.TestData;
import com.tlim.character.Character;
import com.tlim.character.CharacterRepository;
import com.tlim.creature.Creature;
import com.tlim.creature.CreatureRepository;
import com.tlim.hunt.dto.HuntImportRequest;
import com.tlim.hunt.dto.HuntSessionItemResponse;
import com.tlim.hunt.dto.HuntSessionResponse;
import com.tlim.hunt.parser.HuntLogFixtures;
import com.tlim.hunt.parser.JsonHuntParser;
import com.tlim.hunt.parser.TextHuntParser;
import com.tlim.inventory.Inventory;
import com.tlim.inventory.InventoryRepository;
import com.tlim.item.Item;
import com.tlim.item.ItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HuntImportServiceTest {

    private static final long OWNER_ID = 1L;
    private static final long CHARACTER_ID = 10L;

    @Mock HuntSessionRepository huntSessionRepository;
    @Mock HuntSessionItemRepository huntSessionItemRepository;
    @Mock HuntSessionMonsterRepository huntSessionMonsterRepository;
    @Mock ItemRepository itemRepository;
    @Mock CreatureRepository creatureRepository;
    @Mock CharacterRepository characterRepository;
    @Mock InventoryRepository inventoryRepository;

    private HuntImportService service;
    private Character character;

    // In-memory catalog keyed by lowercase in-game name, and inventory keyed by item id
    private final Map<String, Item> itemsByActualName = new HashMap<>();
    private final Map<String, Creature> creaturesByActualName = new HashMap<>();
    private final Map<Long, Inventory> inventoryByItemId = new HashMap<>();

    @BeforeEach
    void setUp() {
        service = new HuntImportService(new TextHuntParser(), new JsonHuntParser(new ObjectMapper()),
                huntSessionRepository, huntSessionItemRepository, huntSessionMonsterRepository,
                itemRepository, creatureRepository, characterRepository, inventoryRepository);

        character = TestData.character(CHARACTER_ID, TestData.user(OWNER_ID), TestData.server(1, "Antica"));
        when(characterRepository.findById(CHARACTER_ID)).thenReturn(Optional.of(character));

        // Display names as the TibiaWiki scraper stores them; actualName is lowercase
        addItem(1, "Gold Coin");
        addItem(2, "Great Spirit Potion");
        addCreature(1, "Bog Raider");
        addCreature(2, "Rotworm");

        when(itemRepository.findFirstByActualNameIgnoreCase(anyString()))
                .thenAnswer(inv -> Optional.ofNullable(itemsByActualName.get(inv.<String>getArgument(0).toLowerCase())));
        when(itemRepository.findFirstByNameIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(creatureRepository.findFirstByActualNameIgnoreCase(anyString()))
                .thenAnswer(inv -> Optional.ofNullable(creaturesByActualName.get(inv.<String>getArgument(0).toLowerCase())));
        when(creatureRepository.findFirstByNameIgnoreCase(anyString())).thenReturn(Optional.empty());

        when(huntSessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(huntSessionItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(huntSessionMonsterRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        when(inventoryRepository.findByCharacterIdAndItemId(anyLong(), anyLong()))
                .thenAnswer(inv -> Optional.ofNullable(inventoryByItemId.get(inv.<Long>getArgument(1))));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(inv -> {
            Inventory saved = inv.getArgument(0);
            inventoryByItemId.put(saved.getItem().getId(), saved);
            return saved;
        });
    }

    private void addItem(long id, String name) {
        Item item = TestData.item(id, name);
        itemsByActualName.put(item.getActualName(), item);
    }

    private void addCreature(long id, String name) {
        Creature creature = TestData.creature(id, name);
        creaturesByActualName.put(creature.getActualName(), creature);
    }

    private static HuntImportRequest request(String rawData) {
        return new HuntImportRequest(CHARACTER_ID, null, rawData, null, null, null,
                null, null, null, null, null, null);
    }

    @Test
    void importAddsLootToInventoryAndReportsUnknownNames() {
        HuntSessionResponse response = service.importFromText(request(HuntLogFixtures.read("small.txt")), OWNER_ID);

        // "belted cape" and "carrion worm" are not in the catalog
        assertThat(response.skippedItems()).containsExactly("belted cape");
        assertThat(response.skippedMonsters()).containsExactly("carrion worm");
        assertThat(response.items()).extracting(HuntSessionItemResponse::itemName)
                .containsExactly("Gold Coin", "Great Spirit Potion");
        assertThat(response.monsters()).hasSize(2);

        assertThat(inventoryByItemId.get(1L).getCurrentQuantity()).isEqualTo(725);
        assertThat(inventoryByItemId.get(1L).getTargetQuantity()).isZero();
        assertThat(inventoryByItemId.get(2L).getCurrentQuantity()).isEqualTo(1);
        assertThat(inventoryByItemId).doesNotContainKey(3L);

        assertThat(response.balance()).isEqualTo(1_479 - 4_543);
        assertThat(response.name()).isEqualTo("Hunt 23/01 18:13");
    }

    @Test
    void secondImportAddsToExistingStockAndKeepsGoal() {
        Inventory existing = TestData.inventory(character, itemsByActualName.get("gold coin"), 100, 50);
        inventoryByItemId.put(1L, existing);

        service.importFromJson(request(HuntLogFixtures.read("small.json")), OWNER_ID);

        assertThat(existing.getCurrentQuantity()).isEqualTo(825);
        assertThat(existing.getTargetQuantity()).isEqualTo(50);
    }

    @Test
    void namesResolvingToTheSameItemAreMerged() {
        String log = HuntLogFixtures.read("small.txt").replace("1x a belted cape", "5x a Gold Coin");

        HuntSessionResponse response = service.importFromText(request(log), OWNER_ID);

        assertThat(response.items()).filteredOn(i -> i.itemId().equals(1L))
                .singleElement()
                .extracting(HuntSessionItemResponse::quantity).isEqualTo(730);
        assertThat(inventoryByItemId.get(1L).getCurrentQuantity()).isEqualTo(730);
    }

    @Test
    void fallsBackToDisplayNameWhenActualNameIsMissing() {
        Item cape = TestData.item(3, "Belted Cape");
        when(itemRepository.findFirstByNameIgnoreCase("belted cape")).thenReturn(Optional.of(cape));

        HuntSessionResponse response = service.importFromText(request(HuntLogFixtures.read("small.txt")), OWNER_ID);

        assertThat(response.skippedItems()).isEmpty();
        assertThat(inventoryByItemId.get(3L).getCurrentQuantity()).isEqualTo(1);
    }

    @Test
    void userSuppliedNameIsKept() {
        HuntImportRequest req = new HuntImportRequest(CHARACTER_ID, "  Rotworm cave ", HuntLogFixtures.read("small.txt"),
                " Morning hunt ", true, "", 100, null, null, null, null, null);

        HuntSessionResponse response = service.importFromText(req, OWNER_ID);

        assertThat(response.name()).isEqualTo("Morning hunt");
        assertThat(response.location()).isEqualTo("Rotworm cave");
        assertThat(response.notes()).isNull();
        assertThat(response.isParty()).isTrue();
    }

    @Test
    void rejectsOtherUsersCharacterBeforePersistingAnything() {
        assertThatThrownBy(() -> service.importFromText(request(HuntLogFixtures.read("small.txt")), 2L))
                .isInstanceOf(AccessDeniedException.class);
        verify(huntSessionRepository, never()).save(any());
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void emptyHuntCreatesSessionWithoutInventoryChanges() {
        HuntSessionResponse response = service.importFromText(request(HuntLogFixtures.read("empty.txt")), OWNER_ID);

        assertThat(response.items()).isEmpty();
        assertThat(response.skippedItems()).isEqualTo(List.of());
        verify(inventoryRepository, never()).save(any());
    }
}
