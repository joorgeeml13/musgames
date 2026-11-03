package dev.ordago.game.infrastructure.ws;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.websockets.next.CloseReason;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnError;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.PathParam;
import io.quarkus.websockets.next.UserData.TypedKey;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import io.vertx.core.Vertx;

@WebSocket(path = "/ws/rooms/{roomId}")
public class GameRoomWebSocketEndpoint {

    private static final Logger LOG = Logger.getLogger(GameRoomWebSocketEndpoint.class);

    public static final int WS_CLOSE_UNAUTHORIZED = 4401;
    public static final String WS_REASON_UNAUTHORIZED = "Unauthorized";
    public static final long AUTH_TIMEOUT_MS = 3000L;

    public static final TypedKey<Boolean> AUTHENTICATED_KEY = TypedKey.forBoolean("ws.authenticated");
    public static final TypedKey<Long> AUTH_TIMER_ID_KEY = TypedKey.forLong("ws.auth_timer_id");
    public static final TypedKey<String> ACCOUNT_ID_KEY = TypedKey.forString("ws.account_id");
    public static final TypedKey<String> PLAYER_TAG_KEY = TypedKey.forString("ws.player_tag");
    public static final TypedKey<String> EMAIL_KEY = TypedKey.forString("ws.email");
    public static final TypedKey<String> ROOM_ID_KEY = TypedKey.forString("ws.room_id");

    private final WebSocketConnection connection;
    private final JWTParser jwtParser;
    private final ObjectMapper objectMapper;
    private final Vertx vertx;

    public GameRoomWebSocketEndpoint(
            WebSocketConnection connection,
            JWTParser jwtParser,
            ObjectMapper objectMapper,
            Vertx vertx
    ) {
        this.connection = connection;
        this.jwtParser = jwtParser;
        this.objectMapper = objectMapper;
        this.vertx = vertx;
    }

    @OnOpen
    public void onOpen(@PathParam("roomId") String roomId) {
        connection.userData().put(AUTHENTICATED_KEY, false);
        String resolvedRoomId = roomId != null && !roomId.isBlank() ? roomId : connection.pathParam("roomId");
        if (resolvedRoomId != null) {
            connection.userData().put(ROOM_ID_KEY, resolvedRoomId);
        }

        long timerId = vertx.setTimer(AUTH_TIMEOUT_MS, tId -> {
            if (connection.isOpen() && !Boolean.TRUE.equals(connection.userData().get(AUTHENTICATED_KEY))) {
                LOG.warnv("Authentication timeout ({0} ms) expired for WebSocket connection {1}",
                        AUTH_TIMEOUT_MS, connection.id());
                closeUnauthorized();
            }
        });
        connection.userData().put(AUTH_TIMER_ID_KEY, timerId);
    }

    @OnTextMessage
    public void onTextMessage(String message) {
        if (!Boolean.TRUE.equals(connection.userData().get(AUTHENTICATED_KEY))) {
            handleAuthentication(message);
            return;
        }
        handleAuthenticatedMessage(message);
    }

    @OnClose
    public void onClose() {
        LOG.debugv("Closing WebSocket connection {0}", connection.id());
        cancelAuthTimer();
    }

    @OnError
    public void onError(Throwable throwable) {
        LOG.warnv("WebSocket error on connection {0}: {1}",
                connection.id(), throwable != null ? throwable.getMessage() : "unknown");
        cancelAuthTimer();
    }

    private void handleAuthentication(String message) {
        if (message == null || message.isBlank()) {
            LOG.warnv("Received empty frame from unauthenticated connection {0}", connection.id());
            closeUnauthorized();
            return;
        }

        WsActionFrame actionFrame;
        try {
            actionFrame = objectMapper.readValue(message, WsActionFrame.class);
        } catch (JsonProcessingException e) {
            LOG.warnv("Malformed JSON from unauthenticated connection {0}: {1}", connection.id(), e.getMessage());
            closeUnauthorized();
            return;
        }

        if (actionFrame == null || !"AUTH".equalsIgnoreCase(actionFrame.action())) {
            LOG.warnv("Unexpected action ''{0}'' from unauthenticated connection {1}",
                    actionFrame != null ? actionFrame.action() : "null", connection.id());
            closeUnauthorized();
            return;
        }

        String token = actionFrame.token();
        if (token == null || token.isBlank()) {
            LOG.warnv("Empty token received for AUTH action from connection {0}", connection.id());
            closeUnauthorized();
            return;
        }

        JsonWebToken jwt;
        try {
            jwt = jwtParser.parse(token);
        } catch (ParseException | RuntimeException e) {
            LOG.warnv("Invalid, malformed or expired JWT from connection {0}: {1}", connection.id(), e.getMessage());
            closeUnauthorized();
            return;
        }

        cancelAuthTimer();
        connection.userData().put(AUTHENTICATED_KEY, true);

        String accountId = jwt.getSubject() != null ? jwt.getSubject() : jwt.getName();
        String playerTag = jwt.getClaim("playerTag");
        String email = jwt.getClaim("email");

        if (accountId != null) {
            connection.userData().put(ACCOUNT_ID_KEY, accountId);
        }
        if (playerTag != null) {
            connection.userData().put(PLAYER_TAG_KEY, playerTag);
        }
        if (email != null) {
            connection.userData().put(EMAIL_KEY, email);
        }

        String roomId = connection.pathParam("roomId");
        if (roomId == null) {
            roomId = connection.userData().get(ROOM_ID_KEY);
        }

        AuthSuccessPayload payload = new AuthSuccessPayload(accountId, playerTag, roomId);
        WsEventFrame authSuccessEvent = new WsEventFrame("AUTH_SUCCESS", payload);

        try {
            String jsonResponse = objectMapper.writeValueAsString(authSuccessEvent);
            connection.sendText(jsonResponse).subscribe().with(
                    v -> LOG.debugv("Sent AUTH_SUCCESS to connection {0}", connection.id()),
                    failure -> LOG.errorv(failure, "Failed to send AUTH_SUCCESS to connection {0}", connection.id())
            );
        } catch (JsonProcessingException e) {
            LOG.errorv(e, "Error serializing AUTH_SUCCESS event for connection {0}", connection.id());
        }
    }

    private void handleAuthenticatedMessage(String message) {
        LOG.debugv("Received frame from authenticated connection {0}: {1}", connection.id(), message);
    }

    private void cancelAuthTimer() {
        Long timerId = connection.userData().remove(AUTH_TIMER_ID_KEY);
        if (timerId != null) {
            vertx.cancelTimer(timerId);
        }
    }

    private void closeUnauthorized() {
        cancelAuthTimer();
        if (connection.isOpen()) {
            connection.close(new CloseReason(WS_CLOSE_UNAUTHORIZED, WS_REASON_UNAUTHORIZED))
                    .subscribe().with(
                            v -> LOG.debugv("Closed unauthorized connection {0}", connection.id()),
                            failure -> LOG.debugv("Error while closing unauthorized connection {0}: {1}",
                                    connection.id(), failure.getMessage())
                    );
        }
    }
}
