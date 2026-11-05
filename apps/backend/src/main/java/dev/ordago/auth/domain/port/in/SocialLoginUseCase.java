package dev.ordago.auth.domain.port.in;

import dev.ordago.auth.domain.model.OAuthProvider;
import dev.ordago.auth.domain.port.in.RegisterUseCase.AuthResult;

public interface SocialLoginUseCase {

    record SocialLoginCommand(
            OAuthProvider provider,
            String token,
            String deviceId,
            String deviceName,
            String clientIp
    ) {}

    AuthResult socialLogin(SocialLoginCommand command);
}
