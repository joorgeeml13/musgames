package org.mus.player.infrastructure.adapter.output.persistence;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.mus.player.domain.model.Player;
import org.mus.player.domain.port.output.PlayerRepositoryPort;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class PostgresPlayerRepositoryAdapter implements PlayerRepositoryPort {

    @Override
    public List<Player> findTopByElo(int limit) {
        return PlayerJpaEntity.find("order by elo desc, wins desc")
                .page(0, limit)
                .<PlayerJpaEntity>list()
                .stream()
                .map(PlayerJpaEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<Player> findByUsername(String username) {
        return PlayerJpaEntity.<PlayerJpaEntity>find("username", username)
                .firstResultOptional()
                .map(PlayerJpaEntity::toDomain);
    }

    @Override
    public boolean existsByUsername(String username) {
        return PlayerJpaEntity.count("username", username) > 0;
    }

    @Override
    @Transactional
    public Player save(Player player) {
        PlayerJpaEntity entity;
        if (player.getId() != null) {
            entity = PlayerJpaEntity.findById(player.getId());
            if (entity == null) {
                entity = PlayerJpaEntity.fromDomain(player);
                entity.id = null;
            } else {
                entity.username = player.getUsername();
                entity.elo = player.getElo();
                entity.wins = player.getWins();
                entity.losses = player.getLosses();
            }
        } else {
            entity = PlayerJpaEntity.fromDomain(player);
        }
        entity.persist();
        return entity.toDomain();
    }
}
