package com.tlim.item;

import com.tlim.item.dto.ItemRequest;
import com.tlim.item.dto.ItemResponse;
import com.tlim.item.dto.NpcBuyerResponse;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ItemService {

    private final ItemRepository itemRepository;

    public ItemService(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    @Transactional
    public ItemResponse createItem(ItemRequest request) {
        if (itemRepository.findByName(request.name()).isPresent()) {
            throw new IllegalArgumentException("Item name already exists: " + request.name());
        }
        Item item = new Item();
        item.setName(request.name());
        item.setDescription(request.description());
        item.setImageUrl(request.imageUrl());
        item.setWeight(request.weight());
        item.setCategory(request.category());
        item.setQuestItem(request.isQuestItem());
        item.setImbuementMaterial(request.isImbuementMaterial());
        item.setDeliveryItem(request.isDeliveryItem());
        return toResponse(itemRepository.save(item));
    }

    public List<ItemResponse> getAllItems() {
        return itemRepository.findAll().stream().map(this::toResponse).toList();
    }

    public List<ItemResponse> getItemsByCategory(String category) {
        return itemRepository.findByCategory(category).stream().map(this::toResponse).toList();
    }

    public ItemResponse getItemById(Long id) {
        return itemRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Item not found: " + id));
    }

    @Transactional
    public ItemResponse updateItem(Long id, ItemRequest request) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Item not found: " + id));
        item.setName(request.name());
        item.setDescription(request.description());
        item.setImageUrl(request.imageUrl());
        item.setWeight(request.weight());
        item.setCategory(request.category());
        item.setQuestItem(request.isQuestItem());
        item.setImbuementMaterial(request.isImbuementMaterial());
        item.setDeliveryItem(request.isDeliveryItem());
        return toResponse(item);
    }

    @Transactional
    public void deleteItem(Long id) {
        if (!itemRepository.existsById(id)) {
            throw new EntityNotFoundException("Item not found: " + id);
        }
        itemRepository.deleteById(id);
    }

    private ItemResponse toResponse(Item item) {
        List<NpcBuyerResponse> buyers = item.getNpcBuyers().stream()
                .map(b -> new NpcBuyerResponse(b.getId(), b.getNpcName(), b.getLocation(), b.getPrice()))
                .toList();
        return new ItemResponse(
                item.getId(),
                item.getName(),
                item.getWikiUrl(),
                item.getDescription(),
                item.getImageUrl(),
                item.getWeight(),
                item.getCategory(),
                item.isQuestItem(),
                item.isImbuementMaterial(),
                item.isDeliveryItem(),
                item.isTaskItem(),
                buyers,
                item.getUpdatedAt()
        );
    }
}
