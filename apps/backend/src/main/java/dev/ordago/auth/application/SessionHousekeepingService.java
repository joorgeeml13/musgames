package dev.ordago.auth.application;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import dev.ordago.auth.domain.port.in.PurgeExpiredSessionsUseCase;
import dev.ordago.auth.domain.port.out.UserSessionRepositoryPort;
import dev.ordago.shared.domain.exception.ValidationException;

@ApplicationScoped
public class SessionHousekeepingService implements PurgeExpiredSessionsUseCase {

    private static final int MAX_RETENTION_DAYS = 3650;

    private final UserSessionRepositoryPort userSessionRepository;

    public SessionHousekeepingService(UserSessionRepositoryPort userSessionRepository) {
        this.userSessionRepository = Objects.requireNonNull(userSessionRepository, "UserSessionRepositoryPort cannot be null");
    }

    @Override
    @Transactional
    public PurgeResult purgeRevokedOrExpiredSessions(int retentionDays) {
        if (retentionDays < 0 || retentionDays > MAX_RETENTION_DAYS) {
            throw new ValidationException("VALIDATION_ERROR", "Retention days must be between 0 and " + MAX_RETENTION_DAYS + ": " + retentionDays);
        }

        Instant cutoff = Instant.now().minus(Duration.ofDays(retentionDays));
        int purgedCount = userSessionRepository.deleteRevokedOrExpiredOlderThan(cutoff);

        return new PurgeResult(purgedCount, cutoff, retentionDays);
    }
}
