package dev.ordago.auth.domain.port.in;

import java.util.UUID;

public interface LogoutUseCase {

    record LogoutCommand(
            String refreshToken,
            UUID sessionId
    ) {}

    void logout(LogoutCommand command);
}
