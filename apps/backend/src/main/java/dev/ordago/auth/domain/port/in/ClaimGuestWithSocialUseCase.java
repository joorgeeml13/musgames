package dev.ordago.auth.domain.port.in;

import java.util.UUID;
import dev.ordago.auth.domain.model.OAuthProvider;
import dev.ordago.auth.domain.port.in.RegisterUseCase.AuthResult;

public interface ClaimGuestWithSocialUseCase {

    record ClaimGuestSocialCommand(
            UUID guestAccountId,
            OAuthProvider provider,
            String token,
            String deviceId,
            String deviceName,
            String clientIp
    ) {}

    AuthResult claimGuestWithSocial(ClaimGuestSocialCommand command);
}
