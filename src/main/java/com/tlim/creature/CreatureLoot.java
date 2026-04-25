package com.tlim.creature;

import com.tlim.item.Item;
import jakarta.persistence.*;

@Entity
@Table(
    name = "creature_loot",
    uniqueConstraints = @UniqueConstraint(columnNames = {"creature_id", "item_id"})
)
public class CreatureLoot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creature_id", nullable = false)
    private Creature creature;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(length = 50)
    private String rarity;

    // Default 1; scraper sets actual range values during upsert
    @Column(name = "min_amount", nullable = false)
    private int minAmount = 1;

    @Column(name = "max_amount", nullable = false)
    private int maxAmount = 1;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Creature getCreature() { return creature; }
    public void setCreature(Creature creature) { this.creature = creature; }

    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }

    public String getRarity() { return rarity; }
    public void setRarity(String rarity) { this.rarity = rarity; }

    public int getMinAmount() { return minAmount; }
    public void setMinAmount(int minAmount) { this.minAmount = minAmount; }

    public int getMaxAmount() { return maxAmount; }
    public void setMaxAmount(int maxAmount) { this.maxAmount = maxAmount; }
}