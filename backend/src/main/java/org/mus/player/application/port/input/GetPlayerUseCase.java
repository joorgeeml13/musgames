package org.mus.player.application.port.input;

import org.mus.player.application.dto.PlayerDto;

public interface GetPlayerUseCase {
    PlayerDto getPlayer(String username);
}
