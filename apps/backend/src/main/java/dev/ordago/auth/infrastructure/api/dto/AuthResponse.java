package dev.ordago.auth.infrastructure.api.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import dev.ordago.auth.domain.model.AuthTokens;
import dev.ordago.auth.domain.port.in.RegisterUseCase.AuthResult;
import dev.ordago.profile.domain.model.UserProfile;

@Schema(description = "Authentication successful response containing tokens and user details")
public record AuthResponse(
        @Schema(description = "Compact signed JWT access token", example = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...")
        String accessToken,

        @Schema(description = "Opaque refresh token string", example = "d9f8e7c6-b5a4-3210-fedc-ba9876543210")
        String refreshToken,

        @Schema(description = "Token type identifier", example = "Bearer")
        String tokenType,

        @Schema(description = "Access token lifespan in seconds", example = "900")
        long expiresIn,

        @Schema(description = "Authenticated user profile information")
        UserInfoResponse user
) {
    public static AuthResponse of(AuthResult authResult, UserProfile profile) {
        AuthTokens tokens = authResult.tokens();
        return new AuthResponse(
                tokens.accessToken(),
                tokens.refreshToken(),
                tokens.tokenType(),
                tokens.expiresInSeconds(),
                UserInfoResponse.of(authResult.account(), profile)
        );
    }
}
