package com.tlim.inventory;

import com.tlim.TestData;
import com.tlim.character.Character;
import com.tlim.character.CharacterRepository;
import com.tlim.inventory.dto.ItemDecisionResponse;
import com.tlim.inventory.dto.SaleDecisionResponse;
import com.tlim.item.Item;
import com.tlim.item.NpcBuyerRepository;
import com.tlim.item.ServerItemPrice;
import com.tlim.item.ServerItemPriceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SaleDecisionServiceTest {

    private static final long SERVER_ID = 7L;
    private static final long CHARACTER_ID = 10L;
    private static final long OWNER_ID = 1L;

    @Mock InventoryRepository inventoryRepository;
    @Mock ServerItemPriceRepository serverItemPriceRepository;
    @Mock NpcBuyerRepository npcBuyerRepository;
    @Mock CharacterRepository characterRepository;
    @InjectMocks SaleDecisionService service;

    private Character character;
    private final List<Inventory> inventory = new ArrayList<>();

    @BeforeEach
    void setUp() {
        character = TestData.character(CHARACTER_ID, TestData.user(OWNER_ID), TestData.server(SERVER_ID, "Antica"));
        when(characterRepository.findById(CHARACTER_ID)).thenReturn(Optional.of(character));
        when(inventoryRepository.findByCharacterId(CHARACTER_ID)).thenReturn(inventory);
        when(npcBuyerRepository.findByItemId(anyLong())).thenReturn(List.of());
        when(serverItemPriceRepository.findByServerIdAndItemId(anyLong(), anyLong())).thenReturn(Optional.empty());
    }

    private Item stock(long id, String name, int current, int target) {
        Item item = TestData.item(id, name);
        inventory.add(TestData.inventory(character, item, current, target));
        return item;
    }

    private void npc(Item item, int... prices) {
        when(npcBuyerRepository.findByItemId(item.getId())).thenReturn(
                java.util.Arrays.stream(prices).mapToObj(p -> TestData.buyer(item, "NPC" + p, p)).toList());
    }

    private void market(Item item, Integer price) {
        ServerItemPrice row = new ServerItemPrice();
        row.setServerId(SERVER_ID);
        row.setItemId(item.getId());
        row.setMarketPrice(price);
        when(serverItemPriceRepository.findByServerIdAndItemId(SERVER_ID, item.getId())).thenReturn(Optional.of(row));
    }

    private SaleDecisionResponse run() {
        return service.calculateDecisions(CHARACTER_ID, OWNER_ID, Optional.empty(), Optional.empty());
    }

    private ItemDecisionResponse only(SaleDecisionResponse response) {
        assertThat(response.items()).hasSize(1);
        return response.items().get(0);
    }

    @Test
    void npcPaysMoreThanMarket() {
        Item item = stock(1, "Dragon Shield", 3, 0);
        npc(item, 360, 4000);
        market(item, 3500);

        ItemDecisionResponse d = only(run());

        assertThat(d.decision()).isEqualTo(SaleDecision.SELL_NPC);
        assertThat(d.bestNpcPrice()).isEqualTo(4000);
        assertThat(d.sellingPrice()).isEqualTo(4000);
        assertThat(d.estimatedValue()).isEqualTo(12_000);   // surplus 3 × 4000
        assertThat(d.goalQuantity()).isNull();
    }

    @Test
    void marketPaysMore() {
        Item item = stock(1, "Belted Cape", 2, 0);
        npc(item, 500);
        market(item, 900);

        ItemDecisionResponse d = only(run());

        assertThat(d.decision()).isEqualTo(SaleDecision.SELL_MARKET);
        assertThat(d.estimatedValue()).isEqualTo(1_800);
    }

    @Test
    void tieGoesToNpc() {
        Item item = stock(1, "Ham", 1, 0);
        npc(item, 4);
        market(item, 4);

        assertThat(only(run()).decision()).isEqualTo(SaleDecision.SELL_NPC);
    }

    @Test
    void marketOnlyItem() {
        Item item = stock(1, "Great Spirit Potion", 5, 0);
        market(item, 300);

        ItemDecisionResponse d = only(run());

        assertThat(d.decision()).isEqualTo(SaleDecision.SELL_MARKET);
        assertThat(d.npcBuyable()).isFalse();
    }

    @Test
    void npcWithoutMarketPriceIsFlagged() {
        Item item = stock(1, "Bola", 115, 0);
        npc(item, 35);

        ItemDecisionResponse d = only(run());

        assertThat(d.decision()).isEqualTo(SaleDecision.SELL_NPC);
        assertThat(d.missingMarketPrice()).isTrue();
        assertThat(d.estimatedValue()).isEqualTo(115L * 35);
    }

    @Test
    void registeredButNullMarketPriceCountsAsMissing() {
        Item item = stock(1, "Lump of Dirt", 3, 0);
        market(item, null);

        assertThat(only(run()).decision()).isEqualTo(SaleDecision.NO_PRICE);
    }

    @Test
    void noPriceAnywhere() {
        stock(1, "Lump of Dirt", 3, 0);

        ItemDecisionResponse d = only(run());

        assertThat(d.decision()).isEqualTo(SaleDecision.NO_PRICE);
        assertThat(d.estimatedValue()).isZero();
    }

    @Test
    void belowGoalKeepsEverything() {
        Item item = stock(1, "Strong Mana Potion", 40, 100);
        npc(item, 50);

        ItemDecisionResponse d = only(run());

        assertThat(d.decision()).isEqualTo(SaleDecision.KEEP);
        assertThat(d.surplusQuantity()).isZero();
        assertThat(d.goalQuantity()).isEqualTo(100);
    }

    @Test
    void onlySurplusAboveGoalIsSold() {
        Item item = stock(1, "Strong Mana Potion", 120, 100);
        npc(item, 50);

        SaleDecisionResponse response = run();
        ItemDecisionResponse d = only(response);

        assertThat(d.surplusQuantity()).isEqualTo(20);
        assertThat(d.estimatedValue()).isEqualTo(1_000);
        assertThat(response.passiveGold()).isEqualTo(1_000);
        // Gross value prices the whole stock, not only the surplus
        assertThat(response.grossValue()).isEqualTo(6_000);
    }

    @Test
    void totalsDoNotOverflowInt() {
        Item item = stock(1, "Gold Ingot", 1_000_000, 0);
        npc(item, 5_000);

        assertThat(run().passiveGold()).isEqualTo(5_000_000_000L);
    }

    @Test
    void defaultOrderPutsActionableItemsFirstAndFilterKeepsTotals() {
        stock(1, "Lump of Dirt", 1, 0);                       // NO_PRICE
        Item keep = stock(2, "Mana Potion", 1, 5);            // KEEP
        npc(keep, 10);
        Item sell = stock(3, "Ham", 2, 0);                    // SELL_NPC
        npc(sell, 4);

        assertThat(run().items()).extracting(ItemDecisionResponse::decision)
                .containsExactly(SaleDecision.SELL_NPC, SaleDecision.NO_PRICE, SaleDecision.KEEP);

        SaleDecisionResponse filtered = service.calculateDecisions(
                CHARACTER_ID, OWNER_ID, Optional.of(SaleDecision.KEEP), Optional.of("name"));
        assertThat(filtered.items()).extracting(ItemDecisionResponse::itemName).containsExactly("Mana Potion");
        assertThat(filtered.passiveGold()).isEqualTo(8);
    }

    @Test
    void usesTheCharactersServerPrices() {
        Item item = stock(1, "Belted Cape", 1, 0);
        market(item, 900);
        when(serverItemPriceRepository.findByServerIdAndItemId(eq(99L), anyLong())).thenReturn(Optional.empty());

        SaleDecisionResponse response = run();

        assertThat(response.serverId()).isEqualTo(SERVER_ID);
        assertThat(only(response).marketPrice()).isEqualTo(900);
    }

    @Test
    void rejectsOtherUsersCharacter() {
        assertThatThrownBy(() -> service.calculateDecisions(CHARACTER_ID, 2L, Optional.empty(), Optional.empty()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void rejectsUnknownSort() {
        assertThatThrownBy(() -> service.calculateDecisions(CHARACTER_ID, OWNER_ID, Optional.empty(), Optional.of("price")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
