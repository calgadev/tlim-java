package com.tlim.inventory;

import com.tlim.character.Character;
import com.tlim.character.CharacterRepository;
import com.tlim.inventory.dto.ItemDecisionResponse;
import com.tlim.inventory.dto.SaleDecisionResponse;
import com.tlim.item.NpcBuyer;
import com.tlim.item.NpcBuyerRepository;
import com.tlim.item.ServerItemPrice;
import com.tlim.item.ServerItemPriceRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class SaleDecisionService {

    // Default ordering: items that need action first
    private static final Map<SaleDecision, Integer> PRIORITY = Map.of(
            SaleDecision.SELL_NPC, 0,
            SaleDecision.SELL_MARKET, 1,
            SaleDecision.NO_PRICE, 2,
            SaleDecision.KEEP, 3);

    private final InventoryRepository inventoryRepository;
    private final ServerItemPriceRepository serverItemPriceRepository;
    private final NpcBuyerRepository npcBuyerRepository;
    private final CharacterRepository characterRepository;

    public SaleDecisionService(InventoryRepository inventoryRepository,
                               ServerItemPriceRepository serverItemPriceRepository,
                               NpcBuyerRepository npcBuyerRepository,
                               CharacterRepository characterRepository) {
        this.inventoryRepository = inventoryRepository;
        this.serverItemPriceRepository = serverItemPriceRepository;
        this.npcBuyerRepository = npcBuyerRepository;
        this.characterRepository = characterRepository;
    }

    /**
     * Computes a sale recommendation for every inventory entry of the character,
     * using market prices from the character's own server.
     *
     * @param decisionFilter only return items with this decision (totals still cover the whole inventory)
     * @param sortBy         "value" (estimatedValue desc), "name", or empty for decision priority
     */
    public SaleDecisionResponse calculateDecisions(Long characterId, Long currentUserId,
                                                   Optional<SaleDecision> decisionFilter,
                                                   Optional<String> sortBy) {
        Character character = characterRepository.findById(characterId)
                .orElseThrow(() -> new EntityNotFoundException("Character not found: " + characterId));
        if (!character.getUser().getId().equals(currentUserId)) {
            throw new AccessDeniedException("Character does not belong to the authenticated user");
        }
        Comparator<ItemDecisionResponse> order = comparatorFor(sortBy);
        Long serverId = character.getServer().getId();

        List<ItemDecisionResponse> items = new ArrayList<>();
        long passiveGold = 0;
        long grossValue = 0;

        for (Inventory inv : inventoryRepository.findByCharacterId(characterId)) {
            ItemDecisionResponse decision = decide(inv, serverId);
            passiveGold += decision.estimatedValue();
            grossValue += (long) decision.quantity() * bestPrice(decision);
            items.add(decision);
        }

        List<ItemDecisionResponse> result = items.stream()
                .filter(d -> decisionFilter.map(f -> d.decision() == f).orElse(true))
                .sorted(order)
                .toList();

        return new SaleDecisionResponse(characterId, serverId, result, passiveGold, grossValue);
    }

    private ItemDecisionResponse decide(Inventory inv, Long serverId) {
        var item = inv.getItem();
        int currentQuantity = inv.getCurrentQuantity();
        int targetQuantity = inv.getTargetQuantity();

        // Surplus rule — targetQuantity 0 means "no goal": the full stock is available
        int surplusQuantity = Math.max(0, currentQuantity - targetQuantity);

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
        int sellingPrice;
        boolean missingMarketPrice = false;

        if (surplusQuantity == 0) {
            decision = SaleDecision.KEEP;
            sellingPrice = 0;
        } else if (!npcBuyable && marketPrice == null) {
            decision = SaleDecision.NO_PRICE;
            sellingPrice = 0;
        } else if (!npcBuyable) {
            decision = SaleDecision.SELL_MARKET;
            sellingPrice = marketPrice;
        } else if (marketPrice == null) {
            // npcBuyable && no market price — flag so caller knows comparison was incomplete
            decision = SaleDecision.SELL_NPC;
            sellingPrice = bestNpcPrice;
            missingMarketPrice = true;
        } else if (bestNpcPrice >= marketPrice) {
            // NPC wins ties: instant sale, no market fee
            decision = SaleDecision.SELL_NPC;
            sellingPrice = bestNpcPrice;
        } else {
            decision = SaleDecision.SELL_MARKET;
            sellingPrice = marketPrice;
        }

        Integer goalQuantity = targetQuantity == 0 ? null : targetQuantity;

        return new ItemDecisionResponse(
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
                sellingPrice,
                (long) surplusQuantity * sellingPrice,
                missingMarketPrice
        );
    }

    // Best known unit price regardless of the goal — used for the gross stock value
    private static int bestPrice(ItemDecisionResponse d) {
        int npc = d.bestNpcPrice() != null ? d.bestNpcPrice() : 0;
        int market = d.marketPrice() != null ? d.marketPrice() : 0;
        return Math.max(npc, market);
    }

    private static Comparator<ItemDecisionResponse> comparatorFor(Optional<String> sortBy) {
        String key = sortBy.map(String::trim).map(String::toLowerCase).orElse("");
        return switch (key) {
            case "value" -> Comparator.comparingLong(ItemDecisionResponse::estimatedValue).reversed()
                    .thenComparing(ItemDecisionResponse::itemName);
            case "name" -> Comparator.comparing(ItemDecisionResponse::itemName, String.CASE_INSENSITIVE_ORDER);
            case "" -> Comparator.<ItemDecisionResponse>comparingInt(d -> PRIORITY.get(d.decision()))
                    .thenComparing(Comparator.comparingLong(ItemDecisionResponse::estimatedValue).reversed());
            default -> throw new IllegalArgumentException("sortBy must be 'value' or 'name'");
        };
    }
}
