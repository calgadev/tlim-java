package com.tlim.hunt;

import com.tlim.TestData;
import com.tlim.character.Character;
import com.tlim.character.CharacterRepository;
import com.tlim.inventory.Inventory;
import com.tlim.inventory.InventoryRepository;
import com.tlim.item.Item;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HuntSessionServiceTest {

    @Mock HuntSessionRepository huntSessionRepository;
    @Mock HuntSessionItemRepository huntSessionItemRepository;
    @Mock HuntSessionMonsterRepository huntSessionMonsterRepository;
    @Mock CharacterRepository characterRepository;
    @Mock InventoryRepository inventoryRepository;
    @InjectMocks HuntSessionService service;

    private Character character;
    private HuntSession session;
    private Item goldCoin;
    private Item cape;

    @BeforeEach
    void setUp() {
        character = TestData.character(10, TestData.user(1), TestData.server(1, "Antica"));
        session = new HuntSession();
        session.setId(50L);
        session.setCharacter(character);
        session.setStartedAt(Instant.parse("2026-01-23T18:13:14Z"));
        when(huntSessionRepository.findById(50L)).thenReturn(Optional.of(session));

        goldCoin = TestData.item(1, "Gold Coin");
        cape = TestData.item(2, "Belted Cape");
        when(huntSessionItemRepository.findByHuntSessionId(50L)).thenReturn(List.of(
                sessionItem(goldCoin, 725),
                sessionItem(cape, 1)));
    }

    private HuntSessionItem sessionItem(Item item, int quantity) {
        HuntSessionItem sessionItem = new HuntSessionItem();
        sessionItem.setHuntSession(session);
        sessionItem.setItem(item);
        sessionItem.setQuantity(quantity);
        return sessionItem;
    }

    @Test
    void deleteSubtractsLootFromInventoryWithoutGoingNegative() {
        Inventory gold = TestData.inventory(character, goldCoin, 1_000, 0);
        Inventory capes = TestData.inventory(character, cape, 0, 0);   // already sold
        when(inventoryRepository.findByCharacterIdAndItemId(10L, 1L)).thenReturn(Optional.of(gold));
        when(inventoryRepository.findByCharacterIdAndItemId(10L, 2L)).thenReturn(Optional.of(capes));

        service.deleteSession(50L, 1L);

        assertThat(gold.getCurrentQuantity()).isEqualTo(275);
        assertThat(capes.getCurrentQuantity()).isZero();
        verify(huntSessionItemRepository).deleteAll(any());
        verify(huntSessionMonsterRepository).deleteAll(any());
        verify(huntSessionRepository).delete(session);
    }

    @Test
    void deleteToleratesInventoryEntriesRemovedByTheUser() {
        when(inventoryRepository.findByCharacterIdAndItemId(any(), any())).thenReturn(Optional.empty());

        service.deleteSession(50L, 1L);

        verify(inventoryRepository, never()).save(any());
        verify(huntSessionRepository).delete(session);
    }

    @Test
    void deleteRejectsOtherUsersSession() {
        assertThatThrownBy(() -> service.deleteSession(50L, 2L)).isInstanceOf(AccessDeniedException.class);
        verify(huntSessionRepository, never()).delete(any());
        verify(inventoryRepository, never()).save(any());
    }
}
