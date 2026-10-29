package dev.ordago.auth.domain.port.in;

import dev.ordago.auth.domain.port.in.RegisterUseCase.AuthResult;

public interface LoginUseCase {

    record LoginCommand(
            String email,
            String password,
            String deviceId,
            String deviceName,
            String clientIp
    ) {}

    AuthResult login(LoginCommand command);
}
