package com.tlim.hunt;

import com.tlim.item.Item;
import jakarta.persistence.*;

@Entity
@Table(
    name = "hunt_session_items",
    uniqueConstraints = @UniqueConstraint(columnNames = {"hunt_session_id", "item_id"})
)
public class HuntSessionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hunt_session_id", nullable = false)
    private HuntSession huntSession;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(nullable = false)
    private int quantity;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public HuntSession getHuntSession() { return huntSession; }
    public void setHuntSession(HuntSession huntSession) { this.huntSession = huntSession; }

    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}