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

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 255)
    private String location;

    @Column(name = "is_party", nullable = false)
    private boolean isParty;

    @Column(columnDefinition = "TEXT")
    private String notes;

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

    @Column(name = "char_level")
    private Integer charLevel;

    @Column(name = "ally_ek_level")
    private Integer allyEkLevel;

    @Column(name = "ally_ms_level")
    private Integer allyMsLevel;

    @Column(name = "ally_ed_level")
    private Integer allyEdLevel;

    @Column(name = "ally_rp_level")
    private Integer allyRpLevel;

    @Column(name = "ally_em_level")
    private Integer allyEmLevel;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Character getCharacter() { return character; }
    public void setCharacter(Character character) { this.character = character; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public boolean isParty() { return isParty; }
    public void setParty(boolean isParty) { this.isParty = isParty; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

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

    public Integer getCharLevel() { return charLevel; }
    public void setCharLevel(Integer charLevel) { this.charLevel = charLevel; }

    public Integer getAllyEkLevel() { return allyEkLevel; }
    public void setAllyEkLevel(Integer allyEkLevel) { this.allyEkLevel = allyEkLevel; }

    public Integer getAllyMsLevel() { return allyMsLevel; }
    public void setAllyMsLevel(Integer allyMsLevel) { this.allyMsLevel = allyMsLevel; }

    public Integer getAllyEdLevel() { return allyEdLevel; }
    public void setAllyEdLevel(Integer allyEdLevel) { this.allyEdLevel = allyEdLevel; }

    public Integer getAllyRpLevel() { return allyRpLevel; }
    public void setAllyRpLevel(Integer allyRpLevel) { this.allyRpLevel = allyRpLevel; }

    public Integer getAllyEmLevel() { return allyEmLevel; }
    public void setAllyEmLevel(Integer allyEmLevel) { this.allyEmLevel = allyEmLevel; }
}
