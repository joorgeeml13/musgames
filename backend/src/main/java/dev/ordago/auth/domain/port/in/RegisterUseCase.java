package dev.ordago.auth.domain.port.in;

import dev.ordago.auth.domain.model.Account;
import dev.ordago.auth.domain.model.AuthTokens;
import dev.ordago.auth.domain.model.UserSession;

public interface RegisterUseCase {

    record RegisterCommand(
            String email,
            String password,
            String gameName,
            String tagLine,
            String deviceId,
            String deviceName,
            String clientIp
    ) {}

    record AuthResult(
            Account account,
            UserSession session,
            AuthTokens tokens
    ) {}

    AuthResult register(RegisterCommand command);
}
