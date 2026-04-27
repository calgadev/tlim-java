package com.tlim.item;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface NpcBuyerRepository extends JpaRepository<NpcBuyer, Long> {
    List<NpcBuyer> findByItemId(Long itemId);
    Optional<NpcBuyer> findByItemIdAndNpcName(Long itemId, String npcName);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("DELETE FROM NpcBuyer b WHERE b.item.id = :itemId")
    void deleteAllByItemId(@Param("itemId") Long itemId);
}
