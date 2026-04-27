package com.tlim.server;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "servers")
public class Server {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "pvp_type", nullable = false, length = 30)
    private PvpType pvpType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    private void prePersist() {
        createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public PvpType getPvpType() { return pvpType; }
    public void setPvpType(PvpType pvpType) { this.pvpType = pvpType; }

    public Instant getCreatedAt() { return createdAt; }
}
