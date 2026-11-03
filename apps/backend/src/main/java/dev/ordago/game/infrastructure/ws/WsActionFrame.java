package dev.ordago.game.infrastructure.ws;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WsActionFrame(
        String action,
        String token,
        JsonNode payload
) {
}
