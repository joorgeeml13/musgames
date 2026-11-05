package dev.ordago.auth.domain.port.in;

import java.util.UUID;
import dev.ordago.auth.domain.port.in.RegisterUseCase.AuthResult;

public interface ClaimGuestAccountUseCase {

    record ClaimGuestCommand(
            UUID accountId,
            String email,
            String password,
            String deviceId,
            String deviceName,
            String clientIp,
            String language
    ) {}

    AuthResult claimGuestAccount(ClaimGuestCommand command);
}
