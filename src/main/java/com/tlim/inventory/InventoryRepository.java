package com.tlim.inventory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    List<Inventory> findByCharacterId(Long characterId);
    Optional<Inventory> findByCharacterIdAndItemId(Long characterId, Long itemId);
}
