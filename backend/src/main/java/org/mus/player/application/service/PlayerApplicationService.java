package org.mus.player.application.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.mus.player.application.dto.CreatePlayerCommand;
import org.mus.player.application.dto.PlayerDto;
import org.mus.player.application.port.input.CreatePlayerUseCase;
import org.mus.player.application.port.input.GetLeaderboardUseCase;
import org.mus.player.application.port.input.GetPlayerUseCase;
import org.mus.player.domain.model.Player;
import org.mus.player.domain.port.output.PlayerRepositoryPort;
import org.mus.shared.domain.exception.ConflictException;
import org.mus.shared.domain.exception.EntityNotFoundException;
import org.mus.shared.domain.exception.ValidationException;

import java.util.List;

@ApplicationScoped
public class PlayerApplicationService implements GetLeaderboardUseCase, GetPlayerUseCase, CreatePlayerUseCase {

    private final PlayerRepositoryPort playerRepositoryPort;

    @Inject
    public PlayerApplicationService(PlayerRepositoryPort playerRepositoryPort) {
        this.playerRepositoryPort = playerRepositoryPort;
    }

    @Override
    public List<PlayerDto> getLeaderboard(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        return playerRepositoryPort.findTopByElo(safeLimit)
                .stream()
                .map(PlayerDto::fromDomain)
                .toList();
    }

    @Override
    public PlayerDto getPlayer(String username) {
        if (username == null || username.isBlank()) {
            throw new ValidationException("VALIDATION_ERROR", "username.required");
        }
        return playerRepositoryPort.findByUsername(username)
                .map(PlayerDto::fromDomain)
                .orElseThrow(() -> new EntityNotFoundException("PLAYER_NOT_FOUND", "player.not_found", username));
    }

    @Override
    @Transactional
    public PlayerDto createPlayer(CreatePlayerCommand command) {
        if (command == null || command.username() == null || command.username().isBlank()) {
            throw new ValidationException("VALIDATION_ERROR", "username.required");
        }

        String trimmedUsername = command.username().trim();
        if (trimmedUsername.length() < 3 || trimmedUsername.length() > 50) {
            throw new ValidationException("VALIDATION_ERROR", "username.length");
        }

        if (!trimmedUsername.matches("^[a-zA-Z0-9_.-]+$")) {
            throw new ValidationException("VALIDATION_ERROR", "username.invalid_format");
        }

        if (playerRepositoryPort.existsByUsername(trimmedUsername)) {
            throw new ConflictException("PLAYER_ALREADY_EXISTS", "player.already_exists", trimmedUsername);
        }

        if (command.elo() != null && command.elo() < 0) {
            throw new ValidationException("VALIDATION_ERROR", "player.negative_stats");
        }

        int elo = (command.elo() == null || command.elo() == 0) ? 1200 : command.elo();
        if (elo > 4000) {
            throw new ValidationException("VALIDATION_ERROR", "player.elo_max_exceeded");
        }

        int wins = command.wins() != null ? command.wins() : 0;
        int losses = command.losses() != null ? command.losses() : 0;

        if (wins < 0 || losses < 0) {
            throw new ValidationException("VALIDATION_ERROR", "player.negative_stats");
        }

        if (wins > 100_000 || losses > 100_000) {
            throw new ValidationException("VALIDATION_ERROR", "player.stats_exceeded");
        }

        Player player = new Player(null, trimmedUsername, elo, wins, losses);
        Player saved = playerRepositoryPort.save(player);
        return PlayerDto.fromDomain(saved);
    }
}
