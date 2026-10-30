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
            String clientIp,
            String language
    ) {
        public RegisterCommand(
                String email,
                String password,
                String gameName,
                String tagLine,
                String deviceId,
                String deviceName,
                String clientIp
        ) {
            this(email, password, gameName, tagLine, deviceId, deviceName, clientIp, "es");
        }
    }

    record AuthResult(
            Account account,
            UserSession session,
            AuthTokens tokens
    ) {}

    AuthResult register(RegisterCommand command);
}
