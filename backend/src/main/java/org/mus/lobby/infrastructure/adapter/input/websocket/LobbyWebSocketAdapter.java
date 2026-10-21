package org.mus.lobby.infrastructure.adapter.input.websocket;

import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnError;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.mus.lobby.application.port.input.ProcessLobbyEventUseCase;

@WebSocket(path = "/ws/lobby")
public class LobbyWebSocketAdapter {

    private static final Logger LOG = Logger.getLogger(LobbyWebSocketAdapter.class);

    @Inject
    WebSocketConnection connection;

    @Inject
    ProcessLobbyEventUseCase processLobbyEventUseCase;

    @OnOpen
    public void onOpen() {
        LOG.infof("Lobby client connected: %s", sanitizeLog(connection.id()));
    }

    @OnTextMessage
    public Uni<Void> onTextMessage(String message) {
        return processLobbyEventUseCase.processAndBroadcast(connection.id(), message);
    }

    @OnClose
    public void onClose() {
        LOG.infof("Lobby client disconnected: %s", sanitizeLog(connection.id()));
    }

    @OnError
    public void onError(Throwable throwable) {
        LOG.errorf(throwable, "Lobby WebSocket error for %s: %s",
                sanitizeLog(connection.id()), sanitizeLog(throwable.getMessage()));
    }

    private String sanitizeLog(String input) {
        if (input == null) {
            return "null";
        }
        String clean = input.replaceAll("[\\r\\n\\t\\u001b\\p{Cntrl}]", "_");
        return clean.length() > 120 ? clean.substring(0, 120) + "... [truncated]" : clean;
    }
}
