package org.mus.player.application.port.input;

import org.mus.player.application.dto.PlayerDto;

import java.util.List;

public interface GetLeaderboardUseCase {
    List<PlayerDto> getLeaderboard(int limit);
}
