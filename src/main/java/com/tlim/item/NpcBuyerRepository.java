package com.tlim.item;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NpcBuyerRepository extends JpaRepository<NpcBuyer, Long> {
    List<NpcBuyer> findByItemId(Long itemId);
    Optional<NpcBuyer> findByItemIdAndNpcName(Long itemId, String npcName);
}
