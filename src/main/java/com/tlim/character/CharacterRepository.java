package com.tlim.character;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CharacterRepository extends JpaRepository<Character, Long> {

    List<Character> findByUserId(Long userId);

    boolean existsByUserIdAndName(Long userId, String name);
}
