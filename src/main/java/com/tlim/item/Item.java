package com.tlim.item;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "items")
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String name;

    @Column(name = "wiki_url", length = 500)
    private String wikiUrl;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(precision = 8, scale = 2)
    private BigDecimal weight;

    @Column(length = 100)
    private String category;

    @Column(name = "is_quest_item", nullable = false)
    private boolean isQuestItem;

    @Column(name = "is_imbuement_material", nullable = false)
    private boolean isImbuementMaterial;

    @Column(name = "is_delivery_item", nullable = false)
    private boolean isDeliveryItem;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Buyers are fully owned by the item; deleting an item cascades to its buyers
    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<NpcBuyer> npcBuyers = new ArrayList<>();

    @PrePersist
    private void prePersist() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    private void preUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getWikiUrl() { return wikiUrl; }
    public void setWikiUrl(String wikiUrl) { this.wikiUrl = wikiUrl; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public boolean isQuestItem() { return isQuestItem; }
    public void setQuestItem(boolean questItem) { isQuestItem = questItem; }

    public boolean isImbuementMaterial() { return isImbuementMaterial; }
    public void setImbuementMaterial(boolean imbuementMaterial) { isImbuementMaterial = imbuementMaterial; }

    public boolean isDeliveryItem() { return isDeliveryItem; }
    public void setDeliveryItem(boolean deliveryItem) { isDeliveryItem = deliveryItem; }

    public Instant getCreatedAt() { return createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public List<NpcBuyer> getNpcBuyers() { return npcBuyers; }
    public void setNpcBuyers(List<NpcBuyer> npcBuyers) { this.npcBuyers = npcBuyers; }
}