package com.tlim.creature;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CreatureLootRepository extends JpaRepository<CreatureLoot, Long> {
    List<CreatureLoot> findByCreatureId(Long creatureId);
    Optional<CreatureLoot> findByCreatureIdAndItemId(Long creatureId, Long itemId);
}
