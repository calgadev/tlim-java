package com.tlim.inventory;

import com.tlim.character.Character;
import com.tlim.character.CharacterRepository;
import com.tlim.inventory.dto.InventoryRequest;
import com.tlim.inventory.dto.InventoryResponse;
import com.tlim.item.Item;
import com.tlim.item.ItemRepository;
import jakarta.persistence.EntityNotFoundException;
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
    public InventoryResponse upsertInventory(Long characterId, InventoryRequest request) {
        Character character = characterRepository.findById(characterId)
                .orElseThrow(() -> new EntityNotFoundException("Character not found: " + characterId));
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

    public List<InventoryResponse> getInventoryByCharacter(Long characterId) {
        return inventoryRepository.findByCharacterId(characterId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public InventoryResponse getInventoryById(Long id) {
        return inventoryRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Inventory entry not found: " + id));
    }

    public void deleteInventoryEntry(Long id) {
        if (!inventoryRepository.existsById(id)) {
            throw new EntityNotFoundException("Inventory entry not found: " + id);
        }
        inventoryRepository.deleteById(id);
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
