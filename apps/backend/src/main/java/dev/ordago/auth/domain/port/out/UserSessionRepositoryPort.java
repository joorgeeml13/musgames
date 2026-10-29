package dev.ordago.auth.domain.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import dev.ordago.auth.domain.model.UserSession;

public interface UserSessionRepositoryPort {

    UserSession save(UserSession session);

    Optional<UserSession> findById(UUID id);

    Optional<UserSession> findByRefreshTokenHash(String refreshTokenHash);

    Optional<UserSession> findByPreviousRefreshTokenHash(String previousRefreshTokenHash);

    Optional<UserSession> findByCurrentOrPreviousTokenHash(String tokenHash);

    List<UserSession> findActiveByAccountId(UUID accountId);

    void revokeAllForAccount(UUID accountId, String reason, Instant revokedAt);
}
