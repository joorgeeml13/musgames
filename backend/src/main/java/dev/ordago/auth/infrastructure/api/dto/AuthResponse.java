package dev.ordago.auth.infrastructure.api.dto;

import dev.ordago.auth.domain.model.AuthTokens;
import dev.ordago.auth.domain.port.in.RegisterUseCase.AuthResult;
import dev.ordago.profile.domain.model.UserProfile;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
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
