package dev.ordago.game.infrastructure.ws;

public record WsEventFrame(
        String event,
        Object payload
) {
}
