package dev.ordago.game.infrastructure.ws;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.websockets.next.CloseReason;
import io.quarkus.websockets.next.UserData;
import io.quarkus.websockets.next.WebSocketConnection;
import io.smallrye.mutiny.Uni;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

import static org.assertj.core.api.Assertions.assertThat;

class GameRoomWebSocketEndpointTest {

    private ObjectMapper objectMapper;
    private TestUserData userData;
    private TestVertxStub vertxStub;
    private TestWebSocketConnectionStub connectionStub;
    private TestJwtParserStub jwtParserStub;
    private GameRoomWebSocketEndpoint endpoint;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        userData = new TestUserData();
        vertxStub = new TestVertxStub();
        connectionStub = new TestWebSocketConnectionStub(userData, "test-conn-1", "test-room-42");
        jwtParserStub = new TestJwtParserStub();

        endpoint = new GameRoomWebSocketEndpoint(
                connectionStub.proxy(),
                jwtParserStub.proxy(),
                objectMapper,
                vertxStub.proxy()
        );
    }

    @Nested
    @DisplayName("Lifecycle: @OnOpen and Handshake Timer")
    class OnOpenTests {

        @Test
        @DisplayName("onOpen initializes connection as unauthenticated and schedules 3-second timer")
        void onOpen_initializesUnauthenticatedStateAndSetsTimer() {
            endpoint.onOpen("test-room-42");

            assertThat(userData.get(GameRoomWebSocketEndpoint.AUTHENTICATED_KEY)).isFalse();
            assertThat(userData.get(GameRoomWebSocketEndpoint.ROOM_ID_KEY)).isEqualTo("test-room-42");
            assertThat(userData.get(GameRoomWebSocketEndpoint.AUTH_TIMER_ID_KEY)).isNotNull();
            assertThat(vertxStub.scheduledDelayMs).isEqualTo(3000L);
            assertThat(vertxStub.scheduledHandler).isNotNull();
        }

        @Test
        @DisplayName("Timer callback closes connection with code 4401 if still unauthenticated")
        void timerExpires_closesConnectionWithUnauthorizedCode() {
            endpoint.onOpen("test-room-42");

            // Simulate the 3-second timer expiring
            vertxStub.scheduledHandler.handle(vertxStub.timerIdCounter.get());

            assertThat(connectionStub.closedReason).isNotNull();
            assertThat(connectionStub.closedReason.getCode()).isEqualTo(GameRoomWebSocketEndpoint.WS_CLOSE_UNAUTHORIZED);
            assertThat(connectionStub.closedReason.getMessage()).isEqualTo(GameRoomWebSocketEndpoint.WS_REASON_UNAUTHORIZED);
        }

        @Test
        @DisplayName("Timer callback does nothing if connection was already closed")
        void timerExpires_whenConnectionAlreadyClosed_doesNotCloseAgain() {
            endpoint.onOpen("test-room-42");
            connectionStub.open = false;

            vertxStub.scheduledHandler.handle(vertxStub.timerIdCounter.get());

            assertThat(connectionStub.closedReason).isNull();
        }
    }

    @Nested
    @DisplayName("Authentication: Valid AUTH Frame")
    class ValidAuthTests {

        @Test
        @DisplayName("Valid AUTH frame cancels timer, marks connection authenticated and sends AUTH_SUCCESS")
        void validAuth_authenticatesAndSendsAuthSuccess() throws Exception {
            endpoint.onOpen("test-room-42");
            Long initialTimerId = userData.get(GameRoomWebSocketEndpoint.AUTH_TIMER_ID_KEY);

            jwtParserStub.tokenToReturn = createMockJwt("account-uuid-99", "MusMaster#4567", "player@ordago.dev");

            String authFrameJson = """
                    {
                      "action": "AUTH",
                      "token": "valid.signed.jwt"
                    }
                    """;

            endpoint.onTextMessage(authFrameJson);

            // Timer was canceled
            assertThat(vertxStub.canceledTimers).contains(initialTimerId);
            assertThat(userData.get(GameRoomWebSocketEndpoint.AUTH_TIMER_ID_KEY)).isNull();

            // Marked as authenticated with user data stored
            assertThat(userData.get(GameRoomWebSocketEndpoint.AUTHENTICATED_KEY)).isTrue();
            assertThat(userData.get(GameRoomWebSocketEndpoint.ACCOUNT_ID_KEY)).isEqualTo("account-uuid-99");
            assertThat(userData.get(GameRoomWebSocketEndpoint.PLAYER_TAG_KEY)).isEqualTo("MusMaster#4567");
            assertThat(userData.get(GameRoomWebSocketEndpoint.EMAIL_KEY)).isEqualTo("player@ordago.dev");

            // Not closed
            assertThat(connectionStub.closedReason).isNull();

            // Event AUTH_SUCCESS was sent to client
            assertThat(connectionStub.sentTextMessages).hasSize(1);
            JsonNode responseJson = objectMapper.readTree(connectionStub.sentTextMessages.getFirst());
            assertThat(responseJson.get("event").asText()).isEqualTo("AUTH_SUCCESS");
            assertThat(responseJson.get("payload").get("accountId").asText()).isEqualTo("account-uuid-99");
            assertThat(responseJson.get("payload").get("playerTag").asText()).isEqualTo("MusMaster#4567");
            assertThat(responseJson.get("payload").get("roomId").asText()).isEqualTo("test-room-42");
        }

        @Test
        @DisplayName("Valid AUTH frame with case-insensitive 'auth' action is accepted")
        void validAuth_caseInsensitiveAction_accepted() throws Exception {
            endpoint.onOpen("test-room-42");
            jwtParserStub.tokenToReturn = createMockJwt("acc-1", "Tag#1", "acc1@mus.dev");

            endpoint.onTextMessage("{\"action\": \"auth\", \"token\": \"jwt-token\"}");

            assertThat(userData.get(GameRoomWebSocketEndpoint.AUTHENTICATED_KEY)).isTrue();
            assertThat(connectionStub.sentTextMessages).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Authentication Failures: Rejections with 4401 Unauthorized")
    class AuthFailureTests {

        @Test
        @DisplayName("Invalid or expired JWT cancels timer and closes with 4401")
        void invalidJwt_cancelsTimerAndCloses4401() {
            endpoint.onOpen("test-room-42");
            Long initialTimerId = userData.get(GameRoomWebSocketEndpoint.AUTH_TIMER_ID_KEY);

            jwtParserStub.exceptionToThrow = new ParseException("Invalid signature or expired token");

            endpoint.onTextMessage("{\"action\": \"AUTH\", \"token\": \"expired-or-bad-jwt\"}");

            assertThat(vertxStub.canceledTimers).contains(initialTimerId);
            assertThat(userData.get(GameRoomWebSocketEndpoint.AUTHENTICATED_KEY)).isFalse();
            assertThat(connectionStub.closedReason).isNotNull();
            assertThat(connectionStub.closedReason.getCode()).isEqualTo(4401);
            assertThat(connectionStub.closedReason.getMessage()).isEqualTo("Unauthorized");
            assertThat(connectionStub.sentTextMessages).isEmpty();
        }

        @Test
        @DisplayName("AUTH action with null or empty token cancels timer and closes with 4401")
        void emptyToken_cancelsTimerAndCloses4401() {
            endpoint.onOpen("test-room-42");
            Long initialTimerId = userData.get(GameRoomWebSocketEndpoint.AUTH_TIMER_ID_KEY);

            endpoint.onTextMessage("{\"action\": \"AUTH\", \"token\": \"   \"}");

            assertThat(vertxStub.canceledTimers).contains(initialTimerId);
            assertThat(connectionStub.closedReason).isNotNull();
            assertThat(connectionStub.closedReason.getCode()).isEqualTo(4401);
        }

        @Test
        @DisplayName("Unauthenticated client sending SET_FOCUS frame cancels timer and closes with 4401")
        void nonAuthActionWhileUnauthenticated_cancelsTimerAndCloses4401() {
            endpoint.onOpen("test-room-42");
            Long initialTimerId = userData.get(GameRoomWebSocketEndpoint.AUTH_TIMER_ID_KEY);

            endpoint.onTextMessage("{\"action\": \"SET_FOCUS\", \"payload\": {\"focused\": true}}");

            assertThat(vertxStub.canceledTimers).contains(initialTimerId);
            assertThat(userData.get(GameRoomWebSocketEndpoint.AUTHENTICATED_KEY)).isFalse();
            assertThat(connectionStub.closedReason).isNotNull();
            assertThat(connectionStub.closedReason.getCode()).isEqualTo(4401);
            assertThat(connectionStub.closedReason.getMessage()).isEqualTo("Unauthorized");
        }

        @Test
        @DisplayName("Unauthenticated client sending SEND_SIGNAL frame cancels timer and closes with 4401")
        void sendSignalActionWhileUnauthenticated_cancelsTimerAndCloses4401() {
            endpoint.onOpen("test-room-42");
            Long initialTimerId = userData.get(GameRoomWebSocketEndpoint.AUTH_TIMER_ID_KEY);

            endpoint.onTextMessage("{\"action\": \"SEND_SIGNAL\", \"payload\": {\"signal\": \"SOLOMILLO\"}}");

            assertThat(vertxStub.canceledTimers).contains(initialTimerId);
            assertThat(connectionStub.closedReason).isNotNull();
            assertThat(connectionStub.closedReason.getCode()).isEqualTo(4401);
        }

        @Test
        @DisplayName("Malformed JSON from unauthenticated client cancels timer and closes with 4401")
        void malformedJsonWhileUnauthenticated_cancelsTimerAndCloses4401() {
            endpoint.onOpen("test-room-42");
            Long initialTimerId = userData.get(GameRoomWebSocketEndpoint.AUTH_TIMER_ID_KEY);

            endpoint.onTextMessage("{not-valid-json: hello");

            assertThat(vertxStub.canceledTimers).contains(initialTimerId);
            assertThat(connectionStub.closedReason).isNotNull();
            assertThat(connectionStub.closedReason.getCode()).isEqualTo(4401);
        }

        @Test
        @DisplayName("Empty or whitespace frame from unauthenticated client cancels timer and closes with 4401")
        void emptyFrameWhileUnauthenticated_cancelsTimerAndCloses4401() {
            endpoint.onOpen("test-room-42");
            Long initialTimerId = userData.get(GameRoomWebSocketEndpoint.AUTH_TIMER_ID_KEY);

            endpoint.onTextMessage("   ");

            assertThat(vertxStub.canceledTimers).contains(initialTimerId);
            assertThat(connectionStub.closedReason).isNotNull();
            assertThat(connectionStub.closedReason.getCode()).isEqualTo(4401);
        }
    }

    @Nested
    @DisplayName("Lifecycle: Clean Disconnections (@OnClose & @OnError)")
    class DisconnectionTests {

        @Test
        @DisplayName("@OnClose cancels pending timer to prevent memory leaks")
        void onClose_cancelsPendingTimer() {
            endpoint.onOpen("test-room-42");
            Long initialTimerId = userData.get(GameRoomWebSocketEndpoint.AUTH_TIMER_ID_KEY);

            endpoint.onClose();

            assertThat(vertxStub.canceledTimers).contains(initialTimerId);
            assertThat(userData.get(GameRoomWebSocketEndpoint.AUTH_TIMER_ID_KEY)).isNull();
        }

        @Test
        @DisplayName("@OnError cancels pending timer to prevent memory leaks")
        void onError_cancelsPendingTimer() {
            endpoint.onOpen("test-room-42");
            Long initialTimerId = userData.get(GameRoomWebSocketEndpoint.AUTH_TIMER_ID_KEY);

            endpoint.onError(new RuntimeException("Connection reset by peer"));

            assertThat(vertxStub.canceledTimers).contains(initialTimerId);
            assertThat(userData.get(GameRoomWebSocketEndpoint.AUTH_TIMER_ID_KEY)).isNull();
        }
    }

    @Nested
    @DisplayName("Post-Authentication: Subsequent Frames")
    class AuthenticatedHandlingTests {

        @Test
        @DisplayName("Subsequent frames from authenticated connection are processed without closing")
        void subsequentFrames_remainConnected() {
            endpoint.onOpen("test-room-42");
            jwtParserStub.tokenToReturn = createMockJwt("user-1", "Tag#1", "u1@ordago.dev");
            endpoint.onTextMessage("{\"action\": \"AUTH\", \"token\": \"jwt\"}");

            // Connection is now authenticated
            assertThat(userData.get(GameRoomWebSocketEndpoint.AUTHENTICATED_KEY)).isTrue();
            connectionStub.sentTextMessages.clear();

            // Client sends a subsequent frame
            endpoint.onTextMessage("{\"action\": \"SET_FOCUS\", \"payload\": {\"focused\": true}}");

            assertThat(connectionStub.closedReason).isNull();
        }
    }

    // =========================================================================
    // Helpers & Test Stubs
    // =========================================================================

    private JsonWebToken createMockJwt(String subject, String playerTag, String email) {
        return (JsonWebToken) Proxy.newProxyInstance(
                JsonWebToken.class.getClassLoader(),
                new Class<?>[]{JsonWebToken.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getSubject", "getName" -> subject;
                    case "getClaim" -> {
                        String claimName = (String) args[0];
                        if ("playerTag".equals(claimName)) yield playerTag;
                        if ("email".equals(claimName)) yield email;
                        yield null;
                    }
                    case "toString" -> "MockJsonWebToken[sub=" + subject + "]";
                    default -> null;
                }
        );
    }

    private static class TestUserData implements UserData {
        private final Map<String, Object> map = new HashMap<>();

        @Override
        @SuppressWarnings("unchecked")
        public <VALUE> VALUE get(TypedKey<VALUE> key) {
            return (VALUE) map.get(key.value());
        }

        @Override
        @SuppressWarnings("unchecked")
        public <VALUE> VALUE put(TypedKey<VALUE> key, VALUE value) {
            return (VALUE) map.put(key.value(), value);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <VALUE> VALUE remove(TypedKey<VALUE> key) {
            return (VALUE) map.remove(key.value());
        }

        @Override
        public int size() {
            return map.size();
        }

        @Override
        public void clear() {
            map.clear();
        }
    }

    private static class TestVertxStub {
        final AtomicLong timerIdCounter = new AtomicLong(100L);
        final List<Long> canceledTimers = new ArrayList<>();
        long scheduledDelayMs;
        Handler<Long> scheduledHandler;

        Vertx proxy() {
            return (Vertx) Proxy.newProxyInstance(
                    Vertx.class.getClassLoader(),
                    new Class<?>[]{Vertx.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "setTimer" -> {
                            scheduledDelayMs = (Long) args[0];
                            @SuppressWarnings("unchecked")
                            Handler<Long> handler = (Handler<Long>) args[1];
                            scheduledHandler = handler;
                            yield timerIdCounter.incrementAndGet();
                        }
                        case "cancelTimer" -> {
                            long id = (Long) args[0];
                            canceledTimers.add(id);
                            yield true;
                        }
                        default -> null;
                    }
            );
        }
    }

    private static class TestWebSocketConnectionStub {
        final UserData userData;
        final String id;
        final String roomId;
        final List<String> sentTextMessages = new ArrayList<>();
        boolean open = true;
        CloseReason closedReason;

        TestWebSocketConnectionStub(UserData userData, String id, String roomId) {
            this.userData = userData;
            this.id = id;
            this.roomId = roomId;
        }

        WebSocketConnection proxy() {
            return (WebSocketConnection) Proxy.newProxyInstance(
                    WebSocketConnection.class.getClassLoader(),
                    new Class<?>[]{WebSocketConnection.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "userData" -> userData;
                        case "id" -> id;
                        case "isOpen" -> open;
                        case "isClosed" -> !open;
                        case "pathParam" -> {
                            String paramName = (String) args[0];
                            if ("roomId".equals(paramName)) yield roomId;
                            yield null;
                        }
                        case "close" -> {
                            open = false;
                            if (args != null && args.length > 0 && args[0] instanceof CloseReason cr) {
                                closedReason = cr;
                            } else {
                                closedReason = CloseReason.NORMAL;
                            }
                            yield Uni.createFrom().voidItem();
                        }
                        case "sendText" -> {
                            if (args != null && args.length > 0 && args[0] instanceof String text) {
                                sentTextMessages.add(text);
                            }
                            yield Uni.createFrom().voidItem();
                        }
                        default -> null;
                    }
            );
        }
    }

    private static class TestJwtParserStub {
        JsonWebToken tokenToReturn;
        ParseException exceptionToThrow;

        JWTParser proxy() {
            return (JWTParser) Proxy.newProxyInstance(
                    JWTParser.class.getClassLoader(),
                    new Class<?>[]{JWTParser.class},
                    (proxy, method, args) -> {
                        if ("parse".equals(method.getName())) {
                            if (exceptionToThrow != null) {
                                throw exceptionToThrow;
                            }
                            return tokenToReturn;
                        }
                        return null;
                    }
            );
        }
    }
}
