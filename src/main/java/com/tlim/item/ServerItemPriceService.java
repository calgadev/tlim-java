package com.tlim.item;

import com.tlim.item.dto.ServerItemPriceResponse;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ServerItemPriceService {

    private final ServerItemPriceRepository serverItemPriceRepository;
    private final ItemRepository itemRepository;

    public ServerItemPriceService(ServerItemPriceRepository serverItemPriceRepository,
                                  ItemRepository itemRepository) {
        this.serverItemPriceRepository = serverItemPriceRepository;
        this.itemRepository = itemRepository;
    }

    @Transactional
    public ServerItemPriceResponse upsertMarketPrice(Long serverId, Long itemId, Integer price) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("Item not found: " + itemId));

        ServerItemPrice entity = serverItemPriceRepository
                .findByServerIdAndItemId(serverId, itemId)
                .orElseGet(() -> {
                    ServerItemPrice p = new ServerItemPrice();
                    p.setServerId(serverId);
                    p.setItemId(itemId);
                    return p;
                });

        entity.setMarketPrice(price);
        entity.setUpdatedAt(OffsetDateTime.now());

        return toResponse(serverItemPriceRepository.save(entity), item.getName());
    }

    public List<ServerItemPriceResponse> getPricesByServer(Long serverId) {
        return serverItemPriceRepository.findByServerId(serverId).stream()
                .map(p -> {
                    String itemName = itemRepository.findById(p.getItemId())
                            .map(Item::getName)
                            .orElse(null);
                    return toResponse(p, itemName);
                })
                .toList();
    }

    private ServerItemPriceResponse toResponse(ServerItemPrice p, String itemName) {
        return new ServerItemPriceResponse(
                p.getId(),
                p.getServerId(),
                p.getItemId(),
                itemName,
                p.getMarketPrice()
        );
    }
}
