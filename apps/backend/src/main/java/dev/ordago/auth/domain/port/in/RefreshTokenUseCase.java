package dev.ordago.auth.domain.port.in;

import dev.ordago.auth.domain.model.AuthTokens;

public interface RefreshTokenUseCase {

    record RefreshTokenCommand(
            String refreshToken,
            String clientIp
    ) {}

    AuthTokens refreshToken(RefreshTokenCommand command);
}
