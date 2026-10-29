package dev.ordago.auth.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.enterprise.context.ApplicationScoped;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;

@ApplicationScoped
public class UserSessionPanacheRepository implements PanacheRepositoryBase<UserSessionEntity, UUID> {

    public Optional<UserSessionEntity> findByRefreshTokenHash(String refreshTokenHash) {
        return find("refreshTokenHash", refreshTokenHash).firstResultOptional();
    }

    public Optional<UserSessionEntity> findByPreviousRefreshTokenHash(String previousRefreshTokenHash) {
        return find("previousRefreshTokenHash", previousRefreshTokenHash).firstResultOptional();
    }

    public Optional<UserSessionEntity> findByCurrentOrPreviousTokenHash(String tokenHash) {
        return find("refreshTokenHash = ?1 or previousRefreshTokenHash = ?1", tokenHash).firstResultOptional();
    }

    public List<UserSessionEntity> findActiveByAccountId(UUID accountId, Instant now) {
        return find("accountId = ?1 and isRevoked = false and expiresAt > ?2", accountId, now).list();
    }

    public void revokeAllForAccount(UUID accountId, String reason, Instant revokedAt) {
        update("isRevoked = true, revokedAt = ?1, revocationReason = ?2 where accountId = ?3 and isRevoked = false",
                revokedAt, reason, accountId);
    }
}
