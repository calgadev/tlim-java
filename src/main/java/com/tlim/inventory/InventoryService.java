package com.tlim.inventory;

import com.tlim.character.Character;
import com.tlim.character.CharacterRepository;
import com.tlim.inventory.dto.InventoryRequest;
import com.tlim.inventory.dto.InventoryResponse;
import com.tlim.item.Item;
import com.tlim.item.ItemRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final CharacterRepository characterRepository;
    private final ItemRepository itemRepository;

    public InventoryService(InventoryRepository inventoryRepository,
                            CharacterRepository characterRepository,
                            ItemRepository itemRepository) {
        this.inventoryRepository = inventoryRepository;
        this.characterRepository = characterRepository;
        this.itemRepository = itemRepository;
    }

    @Transactional
    public InventoryResponse upsertInventory(Long characterId, InventoryRequest request, Long currentUserId) {
        Character character = characterRepository.findById(characterId)
                .orElseThrow(() -> new EntityNotFoundException("Character not found: " + characterId));
        if (!character.getUser().getId().equals(currentUserId)) {
            throw new AccessDeniedException("Character does not belong to the authenticated user");
        }
        Item item = itemRepository.findById(request.itemId())
                .orElseThrow(() -> new EntityNotFoundException("Item not found: " + request.itemId()));

        Inventory inventory = inventoryRepository
                .findByCharacterIdAndItemId(characterId, request.itemId())
                .orElse(new Inventory());

        inventory.setCharacter(character);
        inventory.setItem(item);
        inventory.setCurrentQuantity(request.currentQuantity());
        inventory.setTargetQuantity(request.targetQuantity());

        return toResponse(inventoryRepository.save(inventory));
    }

    public List<InventoryResponse> getInventoryByCharacter(Long characterId, Long currentUserId) {
        Character character = characterRepository.findById(characterId)
                .orElseThrow(() -> new EntityNotFoundException("Character not found: " + characterId));
        if (!character.getUser().getId().equals(currentUserId)) {
            throw new AccessDeniedException("Character does not belong to the authenticated user");
        }
        return inventoryRepository.findByCharacterId(characterId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public InventoryResponse getInventoryById(Long id, Long currentUserId) {
        Inventory inv = inventoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Inventory entry not found: " + id));
        // Treat another user's inventory entry as non-existent to prevent data leakage
        if (!inv.getCharacter().getUser().getId().equals(currentUserId)) {
            throw new EntityNotFoundException("Inventory entry not found: " + id);
        }
        return toResponse(inv);
    }

    public void deleteInventoryEntry(Long id, Long currentUserId) {
        Inventory inv = inventoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Inventory entry not found: " + id));
        // Treat another user's inventory entry as non-existent to prevent data leakage
        if (!inv.getCharacter().getUser().getId().equals(currentUserId)) {
            throw new EntityNotFoundException("Inventory entry not found: " + id);
        }
        inventoryRepository.delete(inv);
    }

    private InventoryResponse toResponse(Inventory inv) {
        int deficit = Math.max(0, inv.getTargetQuantity() - inv.getCurrentQuantity());
        int sellableQuantity = Math.max(0, inv.getCurrentQuantity() - inv.getTargetQuantity());
        return new InventoryResponse(
                inv.getId(),
                inv.getCharacter().getId(),
                inv.getItem().getId(),
                inv.getItem().getName(),
                inv.getCurrentQuantity(),
                inv.getTargetQuantity(),
                deficit,
                sellableQuantity,
                inv.getUpdatedAt()
        );
    }
}
