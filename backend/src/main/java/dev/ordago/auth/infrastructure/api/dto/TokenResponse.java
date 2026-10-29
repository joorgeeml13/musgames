package dev.ordago.auth.infrastructure.api.dto;

import dev.ordago.auth.domain.model.AuthTokens;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn
) {
    public static TokenResponse of(AuthTokens tokens) {
        return new TokenResponse(
                tokens.accessToken(),
                tokens.refreshToken(),
                tokens.tokenType(),
                tokens.expiresInSeconds()
        );
    }
}
