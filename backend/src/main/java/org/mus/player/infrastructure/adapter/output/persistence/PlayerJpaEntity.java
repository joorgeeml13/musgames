package org.mus.player.infrastructure.adapter.output.persistence;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.mus.player.domain.model.Player;

@Entity
@Table(name = "players")
public class PlayerJpaEntity extends PanacheEntity {

    @Column(nullable = false, unique = true)
    public String username;

    @Column(nullable = false)
    public int elo = 1200;

    @Column(nullable = false)
    public int wins = 0;

    @Column(nullable = false)
    public int losses = 0;

    public PlayerJpaEntity() {
    }

    public PlayerJpaEntity(String username, int elo, int wins, int losses) {
        this.username = username;
        this.elo = elo;
        this.wins = wins;
        this.losses = losses;
    }

    public Player toDomain() {
        return new Player(this.id, this.username, this.elo, this.wins, this.losses);
    }

    public static PlayerJpaEntity fromDomain(Player player) {
        PlayerJpaEntity entity = new PlayerJpaEntity();
        entity.id = player.getId();
        entity.username = player.getUsername();
        entity.elo = player.getElo();
        entity.wins = player.getWins();
        entity.losses = player.getLosses();
        return entity;
    }
}
