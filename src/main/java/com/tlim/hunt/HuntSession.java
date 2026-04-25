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

    // Values: "OPEN" or "CLOSED"
    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @PrePersist
    private void prePersist() {
        startedAt = Instant.now();
        if (status == null) {
            status = "OPEN";
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Character getCharacter() { return character; }
    public void setCharacter(Character character) { this.character = character; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getStartedAt() { return startedAt; }

    public Instant getEndedAt() { return endedAt; }
    public void setEndedAt(Instant endedAt) { this.endedAt = endedAt; }
}