package com.tlim.item;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServerItemPriceRepository extends JpaRepository<ServerItemPrice, Long> {
    Optional<ServerItemPrice> findByServerIdAndItemId(Long serverId, Long itemId);
    List<ServerItemPrice> findByServerId(Long serverId);
}
