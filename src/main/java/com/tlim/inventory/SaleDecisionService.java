package com.tlim.inventory;

import com.tlim.item.NpcBuyer;
import com.tlim.item.NpcBuyerRepository;
import com.tlim.item.ServerItemPrice;
import com.tlim.item.ServerItemPriceRepository;
import com.tlim.inventory.dto.ItemDecisionResponse;
import com.tlim.inventory.dto.SaleDecisionResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SaleDecisionService {

    private final InventoryRepository inventoryRepository;
    private final ServerItemPriceRepository serverItemPriceRepository;
    private final NpcBuyerRepository npcBuyerRepository;

    public SaleDecisionService(InventoryRepository inventoryRepository,
                               ServerItemPriceRepository serverItemPriceRepository,
                               NpcBuyerRepository npcBuyerRepository) {
        this.inventoryRepository = inventoryRepository;
        this.serverItemPriceRepository = serverItemPriceRepository;
        this.npcBuyerRepository = npcBuyerRepository;
    }

    public SaleDecisionResponse calculateDecisions(Long characterId, Long serverId) {
        List<Inventory> inventoryList = inventoryRepository.findByCharacterId(characterId);

        List<ItemDecisionResponse> items = new ArrayList<>();
        int passiveGold = 0;
        int grossValue = 0;

        for (Inventory inv : inventoryList) {
            var item = inv.getItem();
            int currentQuantity = inv.getCurrentQuantity();
            int targetQuantity = inv.getTargetQuantity();

            // Surplus rule
            int surplusQuantity;
            if (targetQuantity == 0) {
                surplusQuantity = currentQuantity;
            } else if (currentQuantity < targetQuantity) {
                surplusQuantity = 0;
            } else {
                surplusQuantity = currentQuantity - targetQuantity;
            }

            // NPC buyers — highest price wins
            List<NpcBuyer> buyers = npcBuyerRepository.findByItemId(item.getId());
            boolean npcBuyable = !buyers.isEmpty();
            Integer bestNpcPrice = npcBuyable
                    ? buyers.stream().mapToInt(NpcBuyer::getPrice).max().getAsInt()
                    : null;

            // Market price — null when no row exists OR row has null price (not yet registered)
            Integer marketPrice = serverItemPriceRepository
                    .findByServerIdAndItemId(serverId, item.getId())
                    .map(ServerItemPrice::getMarketPrice)
                    .orElse(null);

            // Decision rule — evaluated strictly in this order
            SaleDecision decision;
            int estimatedValue;
            boolean missingMarketPrice = false;

            if (surplusQuantity == 0) {
                decision = SaleDecision.KEEP;
                estimatedValue = 0;
            } else if (!npcBuyable && marketPrice == null) {
                decision = SaleDecision.NO_PRICE;
                estimatedValue = 0;
            } else if (!npcBuyable) {
                decision = SaleDecision.SELL_MARKET;
                estimatedValue = marketPrice;
            } else if (marketPrice == null) {
                // npcBuyable && no market price — flag so caller knows comparison was incomplete
                decision = SaleDecision.SELL_NPC;
                estimatedValue = bestNpcPrice;
                missingMarketPrice = true;
            } else if (bestNpcPrice >= marketPrice) {
                // NPC wins ties: instant sale, no market fee
                decision = SaleDecision.SELL_NPC;
                estimatedValue = bestNpcPrice;
            } else {
                decision = SaleDecision.SELL_MARKET;
                estimatedValue = marketPrice;
            }

            Integer goalQuantity = targetQuantity == 0 ? null : targetQuantity;

            items.add(new ItemDecisionResponse(
                    item.getId(),
                    item.getName(),
                    item.isTaskItem(),
                    currentQuantity,
                    goalQuantity,
                    surplusQuantity,
                    npcBuyable,
                    bestNpcPrice,
                    marketPrice,
                    decision,
                    estimatedValue,
                    missingMarketPrice
            ));

            passiveGold += surplusQuantity * estimatedValue;
            grossValue += currentQuantity * estimatedValue;
        }

        return new SaleDecisionResponse(items, passiveGold, grossValue);
    }
}
