package com.tlim.creature;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CreatureRepository extends JpaRepository<Creature, Long> {
    Optional<Creature> findByName(String name);
}
