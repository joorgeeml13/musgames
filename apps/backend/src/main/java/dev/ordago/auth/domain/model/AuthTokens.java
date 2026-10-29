package dev.ordago.auth.domain.model;

import java.util.Objects;

public record AuthTokens(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInSeconds
) {
    public AuthTokens {
        Objects.requireNonNull(accessToken, "accessToken cannot be null");
        Objects.requireNonNull(refreshToken, "refreshToken cannot be null");
        tokenType = tokenType != null ? tokenType : "Bearer";
    }

    public static AuthTokens of(String accessToken, String refreshToken, long expiresInSeconds) {
        return new AuthTokens(accessToken, refreshToken, "Bearer", expiresInSeconds);
    }
}
