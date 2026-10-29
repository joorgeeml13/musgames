package dev.ordago.auth.infrastructure.api.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import dev.ordago.auth.domain.model.AuthTokens;

@Schema(description = "Token refresh response payload")
public record TokenResponse(
        @Schema(description = "Compact signed JWT access token", example = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...")
        String accessToken,

        @Schema(description = "New opaque refresh token string", example = "d9f8e7c6-b5a4-3210-fedc-ba9876543210")
        String refreshToken,

        @Schema(description = "Token type identifier", example = "Bearer")
        String tokenType,

        @Schema(description = "Access token lifespan in seconds", example = "900")
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
