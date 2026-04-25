package com.tlim.item;

import jakarta.persistence.*;

@Entity
@Table(
    name = "npc_buyers",
    uniqueConstraints = @UniqueConstraint(columnNames = {"item_id", "npc_name"})
)
public class NpcBuyer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Belongs to one item; deletion cascades from the item side
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(name = "npc_name", nullable = false, length = 255)
    private String npcName;

    @Column(length = 255)
    private String location;

    @Column(nullable = false)
    private int price;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }

    public String getNpcName() { return npcName; }
    public void setNpcName(String npcName) { this.npcName = npcName; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public int getPrice() { return price; }
    public void setPrice(int price) { this.price = price; }
}