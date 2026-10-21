package org.mus.player.application.port.input;

import org.mus.player.application.dto.CreatePlayerCommand;
import org.mus.player.application.dto.PlayerDto;

public interface CreatePlayerUseCase {
    PlayerDto createPlayer(CreatePlayerCommand command);
}
