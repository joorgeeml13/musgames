package dev.ordago.auth.domain.port.in;

import dev.ordago.auth.domain.port.in.RegisterUseCase.AuthResult;

public interface GuestLoginUseCase {

    record GuestLoginCommand(String deviceId, String deviceName, String clientIp) {}

    AuthResult loginAsGuest(GuestLoginCommand command);
}
