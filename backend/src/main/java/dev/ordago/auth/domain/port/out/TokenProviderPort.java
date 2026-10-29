package dev.ordago.auth.domain.port.out;

import java.time.Duration;
import dev.ordago.auth.domain.model.Account;
import dev.ordago.auth.domain.model.UserSession;

public interface TokenProviderPort {

    String createAccessToken(Account account, UserSession session);

    String generateRefreshToken();

    String hashToken(String token);

    long getAccessTokenLifespanSeconds();

    Duration getRefreshTokenLifespan();
}
