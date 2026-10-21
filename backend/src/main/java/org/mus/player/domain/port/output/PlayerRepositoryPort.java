package org.mus.player.domain.port.output;

import org.mus.player.domain.model.Player;

import java.util.List;
import java.util.Optional;

public interface PlayerRepositoryPort {
    List<Player> findTopByElo(int limit);
    Optional<Player> findByUsername(String username);
    boolean existsByUsername(String username);
    Player save(Player player);
}
