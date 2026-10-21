package org.mus.lobby.infrastructure.adapter.output.websocket;

import io.quarkus.websockets.next.WebSocketConnection;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.mus.lobby.domain.port.output.LobbyBroadcasterPort;

@ApplicationScoped
public class LobbyWebSocketBroadcasterAdapter implements LobbyBroadcasterPort {

    @Inject
    WebSocketConnection connection;

    @Override
    public Uni<Void> broadcast(String message) {
        return connection.broadcast().sendText(message);
    }
}
