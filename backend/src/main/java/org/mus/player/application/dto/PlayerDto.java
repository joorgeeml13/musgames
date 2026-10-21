package org.mus.player.application.dto;

import org.mus.player.domain.model.Player;

public record PlayerDto(
        Long id,
        String username,
        int elo,
        int wins,
        int losses
) {
    public static PlayerDto fromDomain(Player player) {
        if (player == null) {
            return null;
        }
        return new PlayerDto(
                player.getId(),
                player.getUsername(),
                player.getElo(),
                player.getWins(),
                player.getLosses()
        );
    }
}
