package dev.ordago.game.infrastructure.ws;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.common.http.TestHTTPPort;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.jwt.build.Jwt;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.WebSocket;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class GameRoomWebSocketEndpointIT {

    @TestHTTPPort
    int port;

    @Inject
    Vertx vertx;

    private HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        httpClient = vertx.createHttpClient();
    }

    @AfterEach
    void tearDown() {
        if (httpClient != null) {
            httpClient.close();
        }
    }

    @Test
    @DisplayName("IT: Successful handshake authenticates client and returns AUTH_SUCCESS")
    void testSuccessfulWebSocketAuthHandshake() throws Exception {
        String roomId = "room-" + UUID.randomUUID();
        String accountId = UUID.randomUUID().toString();
        String playerTag = "MusPro#1234";
        String email = "pro@ordago.dev";

        String token = Jwt.issuer("https://mus.org/issuer")
                .subject(accountId)
                .claim("playerTag", playerTag)
                .claim("email", email)
                .sign();

        CompletableFuture<WebSocket> connectFuture = new CompletableFuture<>();
        httpClient.webSocket(port, "localhost", "/ws/rooms/" + roomId, ar -> {
            if (ar.succeeded()) {
                connectFuture.complete(ar.result());
            } else {
                connectFuture.completeExceptionally(ar.cause());
            }
        });

        WebSocket ws = connectFuture.get(5, TimeUnit.SECONDS);

        CompletableFuture<String> responseFuture = new CompletableFuture<>();
        ws.textMessageHandler(responseFuture::complete);

        String authPayload = objectMapper.writeValueAsString(new WsActionFrame("AUTH", token, null));
        ws.writeTextMessage(authPayload);

        String rawResponse = responseFuture.get(5, TimeUnit.SECONDS);
        JsonNode responseNode = objectMapper.readTree(rawResponse);

        assertThat(responseNode.get("event").asText()).isEqualTo("AUTH_SUCCESS");
        assertThat(responseNode.get("payload").get("accountId").asText()).isEqualTo(accountId);
        assertThat(responseNode.get("payload").get("playerTag").asText()).isEqualTo(playerTag);
        assertThat(responseNode.get("payload").get("roomId").asText()).isEqualTo(roomId);

        ws.close();
    }

    @Test
    @DisplayName("IT: Non-AUTH frame sent by unauthenticated client immediately closes with 4401")
    void testNonAuthFrameRejectionClosesWith4401() throws Exception {
        String roomId = "room-" + UUID.randomUUID();

        CompletableFuture<WebSocket> connectFuture = new CompletableFuture<>();
        httpClient.webSocket(port, "localhost", "/ws/rooms/" + roomId, ar -> {
            if (ar.succeeded()) {
                connectFuture.complete(ar.result());
            } else {
                connectFuture.completeExceptionally(ar.cause());
            }
        });

        WebSocket ws = connectFuture.get(5, TimeUnit.SECONDS);

        CompletableFuture<Short> closeCodeFuture = new CompletableFuture<>();
        ws.closeHandler(v -> closeCodeFuture.complete(ws.closeStatusCode()));

        ws.writeTextMessage("{\"action\": \"SET_FOCUS\", \"payload\": {\"focused\": true}}");

        Short closeCode = closeCodeFuture.get(5, TimeUnit.SECONDS);
        assertThat(closeCode).isEqualTo((short) 4401);
    }

    @Test
    @DisplayName("IT: Invalid token sent in AUTH frame immediately closes with 4401")
    void testInvalidTokenRejectionClosesWith4401() throws Exception {
        String roomId = "room-" + UUID.randomUUID();

        CompletableFuture<WebSocket> connectFuture = new CompletableFuture<>();
        httpClient.webSocket(port, "localhost", "/ws/rooms/" + roomId, ar -> {
            if (ar.succeeded()) {
                connectFuture.complete(ar.result());
            } else {
                connectFuture.completeExceptionally(ar.cause());
            }
        });

        WebSocket ws = connectFuture.get(5, TimeUnit.SECONDS);

        CompletableFuture<Short> closeCodeFuture = new CompletableFuture<>();
        ws.closeHandler(v -> closeCodeFuture.complete(ws.closeStatusCode()));

        ws.writeTextMessage("{\"action\": \"AUTH\", \"token\": \"not-a-valid-token\"}");

        Short closeCode = closeCodeFuture.get(5, TimeUnit.SECONDS);
        assertThat(closeCode).isEqualTo((short) 4401);
    }

    @Test
    @DisplayName("IT: Unauthenticated connection times out after 3 seconds with code 4401")
    void testHandshakeTimeoutClosesWith4401() throws Exception {
        String roomId = "room-" + UUID.randomUUID();

        CompletableFuture<WebSocket> connectFuture = new CompletableFuture<>();
        httpClient.webSocket(port, "localhost", "/ws/rooms/" + roomId, ar -> {
            if (ar.succeeded()) {
                connectFuture.complete(ar.result());
            } else {
                connectFuture.completeExceptionally(ar.cause());
            }
        });

        WebSocket ws = connectFuture.get(5, TimeUnit.SECONDS);

        CompletableFuture<Short> closeCodeFuture = new CompletableFuture<>();
        ws.closeHandler(v -> closeCodeFuture.complete(ws.closeStatusCode()));

        // Do not send AUTH frame, wait for 3s timer to fire
        Short closeCode = closeCodeFuture.get(6, TimeUnit.SECONDS);
        assertThat(closeCode).isEqualTo((short) 4401);
    }
}
