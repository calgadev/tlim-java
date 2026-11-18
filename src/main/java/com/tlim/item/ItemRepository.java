package com.tlim.item;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ItemRepository extends JpaRepository<Item, Long> {
    Optional<Item> findByName(String name);
    Optional<Item> findFirstByNameIgnoreCase(String name);
    Optional<Item> findFirstByActualNameIgnoreCase(String actualName);
    List<Item> findByCategory(String category);
    List<Item> findByNameContainingIgnoreCase(String name);
}
