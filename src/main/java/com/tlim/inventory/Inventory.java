package com.tlim.inventory;

import com.tlim.character.Character;
import com.tlim.item.Item;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
    name = "inventory",
    uniqueConstraints = @UniqueConstraint(columnNames = {"character_id", "item_id"})
)
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "character_id", nullable = false)
    private Character character;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(name = "current_quantity", nullable = false)
    private int currentQuantity;

    @Column(name = "target_quantity", nullable = false)
    private int targetQuantity;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    private void touch() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Character getCharacter() { return character; }
    public void setCharacter(Character character) { this.character = character; }

    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }

    public int getCurrentQuantity() { return currentQuantity; }
    public void setCurrentQuantity(int currentQuantity) { this.currentQuantity = currentQuantity; }

    public int getTargetQuantity() { return targetQuantity; }
    public void setTargetQuantity(int targetQuantity) { this.targetQuantity = targetQuantity; }

    public Instant getUpdatedAt() { return updatedAt; }
}