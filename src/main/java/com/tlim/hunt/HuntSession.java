package com.tlim.hunt;

import com.tlim.character.Character;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "hunt_sessions")
public class HuntSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "character_id", nullable = false)
    private Character character;

    @Column(length = 255)
    private String location;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ended_at", nullable = false)
    private Instant endedAt;

    @Column(nullable = false, length = 50)
    private String duration;

    @Column(name = "raw_xp", nullable = false)
    private int rawXp;

    @Column(name = "xp_with_bonus", nullable = false)
    private int xpWithBonus;

    @Column(name = "loot_total", nullable = false)
    private int lootTotal;

    @Column(nullable = false)
    private int supplies;

    @Column(nullable = false)
    private int damage;

    @Column(nullable = false)
    private int healing;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Character getCharacter() { return character; }
    public void setCharacter(Character character) { this.character = character; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getEndedAt() { return endedAt; }
    public void setEndedAt(Instant endedAt) { this.endedAt = endedAt; }

    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }

    public int getRawXp() { return rawXp; }
    public void setRawXp(int rawXp) { this.rawXp = rawXp; }

    public int getXpWithBonus() { return xpWithBonus; }
    public void setXpWithBonus(int xpWithBonus) { this.xpWithBonus = xpWithBonus; }

    public int getLootTotal() { return lootTotal; }
    public void setLootTotal(int lootTotal) { this.lootTotal = lootTotal; }

    public int getSupplies() { return supplies; }
    public void setSupplies(int supplies) { this.supplies = supplies; }

    public int getDamage() { return damage; }
    public void setDamage(int damage) { this.damage = damage; }

    public int getHealing() { return healing; }
    public void setHealing(int healing) { this.healing = healing; }
}
