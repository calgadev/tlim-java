package com.tlim.creature;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "creatures")
public class Creature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String name;

    @Column(name = "wiki_url", length = 500)
    private String wikiUrl;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    private Integer hp;
    private Integer experience;

    // Nullable: null means the resistance value was not present on the wiki page
    @Column(name = "physical_resistance")
    private Integer physicalResistance;

    @Column(name = "fire_resistance")
    private Integer fireResistance;

    @Column(name = "ice_resistance")
    private Integer iceResistance;

    @Column(name = "energy_resistance")
    private Integer energyResistance;

    @Column(name = "earth_resistance")
    private Integer earthResistance;

    @Column(name = "death_resistance")
    private Integer deathResistance;

    @Column(name = "holy_resistance")
    private Integer holyResistance;

    @Column(name = "drown_resistance")
    private Integer drownResistance;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

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

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public Integer getHp() { return hp; }
    public void setHp(Integer hp) { this.hp = hp; }

    public Integer getExperience() { return experience; }
    public void setExperience(Integer experience) { this.experience = experience; }

    public Integer getPhysicalResistance() { return physicalResistance; }
    public void setPhysicalResistance(Integer physicalResistance) { this.physicalResistance = physicalResistance; }

    public Integer getFireResistance() { return fireResistance; }
    public void setFireResistance(Integer fireResistance) { this.fireResistance = fireResistance; }

    public Integer getIceResistance() { return iceResistance; }
    public void setIceResistance(Integer iceResistance) { this.iceResistance = iceResistance; }

    public Integer getEnergyResistance() { return energyResistance; }
    public void setEnergyResistance(Integer energyResistance) { this.energyResistance = energyResistance; }

    public Integer getEarthResistance() { return earthResistance; }
    public void setEarthResistance(Integer earthResistance) { this.earthResistance = earthResistance; }

    public Integer getDeathResistance() { return deathResistance; }
    public void setDeathResistance(Integer deathResistance) { this.deathResistance = deathResistance; }

    public Integer getHolyResistance() { return holyResistance; }
    public void setHolyResistance(Integer holyResistance) { this.holyResistance = holyResistance; }

    public Integer getDrownResistance() { return drownResistance; }
    public void setDrownResistance(Integer drownResistance) { this.drownResistance = drownResistance; }

    public Instant getCreatedAt() { return createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}