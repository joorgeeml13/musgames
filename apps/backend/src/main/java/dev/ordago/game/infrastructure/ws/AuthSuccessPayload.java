package dev.ordago.game.infrastructure.ws;

public record AuthSuccessPayload(
        String accountId,
        String playerTag,
        String roomId
) {
}
