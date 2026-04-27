package com.tlim.item;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "server_item_prices")
public class ServerItemPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "server_id", nullable = false)
    private Long serverId;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "market_price")
    private Integer marketPrice;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getServerId() { return serverId; }
    public void setServerId(Long serverId) { this.serverId = serverId; }

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }

    public Integer getMarketPrice() { return marketPrice; }
    public void setMarketPrice(Integer marketPrice) { this.marketPrice = marketPrice; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
