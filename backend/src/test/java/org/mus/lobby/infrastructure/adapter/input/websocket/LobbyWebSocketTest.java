package org.mus.lobby.infrastructure.adapter.input.websocket;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.vertx.core.Vertx;
import io.vertx.core.http.WebSocketClient;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class LobbyWebSocketTest {

    @TestHTTPResource("/ws/lobby")
    URI uri;

    @Inject
    Vertx vertx;

    @Test
    void testLobbyWebSocketEcho() throws Exception {
        WebSocketClient client = vertx.createWebSocketClient();
        CompletableFuture<String> receivedMessage = new CompletableFuture<>();

        client.connect(uri.getPort(), uri.getHost(), uri.getPath())
                .onSuccess(ws -> {
                    ws.textMessageHandler(receivedMessage::complete);
                    ws.writeTextMessage("Hello Lobby");
                })
                .onFailure(receivedMessage::completeExceptionally);

        String response = receivedMessage.get(10, TimeUnit.SECONDS);
        assertEquals("Hello Lobby", response);
        client.close();
    }

    @Test
    void testLobbyWebSocketBroadcastMultiClient() throws Exception {
        WebSocketClient client1 = vertx.createWebSocketClient();
        WebSocketClient client2 = vertx.createWebSocketClient();
        CompletableFuture<String> client2Received = new CompletableFuture<>();
        CompletableFuture<Void> client2Connected = new CompletableFuture<>();

        client2.connect(uri.getPort(), uri.getHost(), uri.getPath())
                .onSuccess(ws2 -> {
                    ws2.textMessageHandler(client2Received::complete);
                    client2Connected.complete(null);
                })
                .onFailure(client2Connected::completeExceptionally);

        client2Connected.get(10, TimeUnit.SECONDS);

        CompletableFuture<Void> client1Sent = new CompletableFuture<>();
        client1.connect(uri.getPort(), uri.getHost(), uri.getPath())
                .onSuccess(ws1 -> {
                    ws1.writeTextMessage("Player A joined the table");
                    client1Sent.complete(null);
                })
                .onFailure(client1Sent::completeExceptionally);

        client1Sent.get(10, TimeUnit.SECONDS);

        String broadcasted = client2Received.get(10, TimeUnit.SECONDS);
        assertEquals("Player A joined the table", broadcasted);

        client1.close();
        client2.close();
    }
}
