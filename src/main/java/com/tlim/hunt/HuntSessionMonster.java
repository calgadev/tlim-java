package com.tlim.hunt;

import com.tlim.creature.Creature;
import jakarta.persistence.*;

@Entity
@Table(
    name = "hunt_session_monsters",
    uniqueConstraints = @UniqueConstraint(columnNames = {"hunt_session_id", "creature_id"})
)
public class HuntSessionMonster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hunt_session_id", nullable = false)
    private HuntSession huntSession;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creature_id", nullable = false)
    private Creature creature;

    @Column(name = "kill_count", nullable = false)
    private int killCount;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public HuntSession getHuntSession() { return huntSession; }
    public void setHuntSession(HuntSession huntSession) { this.huntSession = huntSession; }

    public Creature getCreature() { return creature; }
    public void setCreature(Creature creature) { this.creature = creature; }

    public int getKillCount() { return killCount; }
    public void setKillCount(int killCount) { this.killCount = killCount; }
}