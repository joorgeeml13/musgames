package org.mus.lobby.application.port.input;

import io.smallrye.mutiny.Uni;

public interface ProcessLobbyEventUseCase {
    Uni<Void> processAndBroadcast(String connectionId, String rawMessage);
}
