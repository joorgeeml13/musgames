package dev.ordago.auth.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import jakarta.enterprise.context.ApplicationScoped;
import dev.ordago.auth.domain.model.UserSession;
import dev.ordago.auth.domain.port.out.UserSessionRepositoryPort;

@ApplicationScoped
public class UserSessionRepositoryAdapter implements UserSessionRepositoryPort {

    private final UserSessionPanacheRepository panacheRepository;

    public UserSessionRepositoryAdapter(UserSessionPanacheRepository panacheRepository) {
        this.panacheRepository = Objects.requireNonNull(panacheRepository, "UserSessionPanacheRepository cannot be null");
    }

    @Override
    public UserSession save(UserSession session) {
        UserSessionEntity entity = panacheRepository.findById(session.getId());
        if (entity == null) {
            entity = UserSessionEntity.fromDomain(session);
            panacheRepository.persist(entity);
        } else {
            entity.refreshTokenHash = session.getRefreshTokenHash();
            entity.previousRefreshTokenHash = session.getPreviousRefreshTokenHash();
            entity.expiresAt = session.getExpiresAt();
            entity.isRevoked = session.isRevoked();
            entity.revokedAt = session.getRevokedAt();
            entity.revocationReason = session.getRevocationReason();
            entity.lastRefreshedAt = session.getLastRefreshedAt();
        }
        return entity.toDomain();
    }

    @Override
    public Optional<UserSession> findById(UUID id) {
        return panacheRepository.findByIdOptional(id).map(UserSessionEntity::toDomain);
    }

    @Override
    public Optional<UserSession> findByRefreshTokenHash(String refreshTokenHash) {
        return panacheRepository.findByRefreshTokenHash(refreshTokenHash).map(UserSessionEntity::toDomain);
    }

    @Override
    public Optional<UserSession> findByPreviousRefreshTokenHash(String previousRefreshTokenHash) {
        return panacheRepository.findByPreviousRefreshTokenHash(previousRefreshTokenHash).map(UserSessionEntity::toDomain);
    }

    @Override
    public Optional<UserSession> findByCurrentOrPreviousTokenHash(String tokenHash) {
        return panacheRepository.findByCurrentOrPreviousTokenHash(tokenHash).map(UserSessionEntity::toDomain);
    }

    @Override
    public List<UserSession> findActiveByAccountId(UUID accountId) {
        return panacheRepository.findActiveByAccountId(accountId, Instant.now())
                .stream()
                .map(UserSessionEntity::toDomain)
                .toList();
    }

    @Override
    public void revokeAllForAccount(UUID accountId, String reason, Instant revokedAt) {
        panacheRepository.revokeAllForAccount(accountId, reason, revokedAt);
    }
}
