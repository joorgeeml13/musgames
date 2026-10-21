package org.mus.lobby.domain.port.output;

import io.smallrye.mutiny.Uni;

public interface LobbyBroadcasterPort {
    Uni<Void> broadcast(String message);
}
