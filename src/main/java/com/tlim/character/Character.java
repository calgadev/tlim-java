package com.tlim.character;

import com.tlim.server.Server;
import com.tlim.user.User;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
    name = "characters",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "name"})
)
public class Character {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "server_id", nullable = false)
    private Server server;

    @Enumerated(EnumType.STRING)
    @Column(length = 2)
    private Vocation vocation;

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

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Server getServer() { return server; }
    public void setServer(Server server) { this.server = server; }

    public Vocation getVocation() { return vocation; }
    public void setVocation(Vocation vocation) { this.vocation = vocation; }

    public Instant getCreatedAt() { return createdAt; }
}
