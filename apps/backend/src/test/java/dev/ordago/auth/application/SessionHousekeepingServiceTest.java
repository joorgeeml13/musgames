package dev.ordago.auth.application;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import dev.ordago.auth.domain.model.UserSession;
import dev.ordago.auth.domain.port.in.PurgeExpiredSessionsUseCase.PurgeResult;
import dev.ordago.auth.domain.port.out.UserSessionRepositoryPort;
import dev.ordago.shared.domain.exception.ValidationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SessionHousekeepingServiceTest {

    private SessionHousekeepingService housekeepingService;
    private StubUserSessionRepository sessionRepository;

    @BeforeEach
    void setUp() {
        sessionRepository = new StubUserSessionRepository();
        housekeepingService = new SessionHousekeepingService(sessionRepository);
    }

    @Test
    @DisplayName("Should purge revoked or expired sessions older than retention days")
    void shouldPurgeExpiredAndRevokedSessionsSuccessfully() {
        // Arrange
        sessionRepository.deleteReturnValue = 5;
        Instant beforeCall = Instant.now();

        // Act
        PurgeResult result = housekeepingService.purgeRevokedOrExpiredSessions(30);

        // Assert
        Instant afterCall = Instant.now();

        assertThat(result).isNotNull();
        assertThat(result.purgedCount()).isEqualTo(5);
        assertThat(result.retentionDays()).isEqualTo(30);
        assertThat(sessionRepository.deleteCallCount).isEqualTo(1);
        assertThat(sessionRepository.lastCutoff).isNotNull();
        assertThat(sessionRepository.lastCutoff).isEqualTo(result.cutoff());

        Instant expectedMinCutoff = beforeCall.minus(Duration.ofDays(30));
        Instant expectedMaxCutoff = afterCall.minus(Duration.ofDays(30));
        assertThat(result.cutoff()).isBetween(expectedMinCutoff, expectedMaxCutoff);
    }

    @Test
    @DisplayName("Should support zero retention days for immediate purge of past expired/revoked sessions")
    void shouldPurgeSessionsWithZeroRetentionDays() {
        // Arrange
        sessionRepository.deleteReturnValue = 2;
        Instant beforeCall = Instant.now();

        // Act
        PurgeResult result = housekeepingService.purgeRevokedOrExpiredSessions(0);

        // Assert
        Instant afterCall = Instant.now();

        assertThat(result.purgedCount()).isEqualTo(2);
        assertThat(result.retentionDays()).isEqualTo(0);
        assertThat(result.cutoff()).isBetween(beforeCall, afterCall);
        assertThat(sessionRepository.deleteCallCount).isEqualTo(1);
    }

    @Test
    @DisplayName("Should return zero purged count when no sessions match cleanup criteria")
    void shouldReturnZeroWhenNoSessionsPurged() {
        // Arrange
        sessionRepository.deleteReturnValue = 0;

        // Act
        PurgeResult result = housekeepingService.purgeRevokedOrExpiredSessions(15);

        // Assert
        assertThat(result.purgedCount()).isZero();
        assertThat(result.retentionDays()).isEqualTo(15);
        assertThat(sessionRepository.deleteCallCount).isEqualTo(1);
    }

    @Test
    @DisplayName("Should throw ValidationException when retention days is negative")
    void shouldThrowValidationExceptionWhenRetentionDaysIsNegative() {
        // Arrange & Act & Assert
        assertThatThrownBy(() -> housekeepingService.purgeRevokedOrExpiredSessions(-1))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Retention days must be between 0 and 3650");

        assertThatThrownBy(() -> housekeepingService.purgeRevokedOrExpiredSessions(-30))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Retention days must be between 0 and 3650");

        assertThat(sessionRepository.deleteCallCount).isZero();
    }

    @Test
    @DisplayName("Should throw ValidationException when retention days exceeds maximum allowed 3650")
    void shouldThrowValidationExceptionWhenRetentionDaysExceedsMaximum() {
        // Arrange & Act & Assert
        assertThatThrownBy(() -> housekeepingService.purgeRevokedOrExpiredSessions(3651))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Retention days must be between 0 and 3650");

        assertThatThrownBy(() -> housekeepingService.purgeRevokedOrExpiredSessions(10000))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Retention days must be between 0 and 3650");

        assertThat(sessionRepository.deleteCallCount).isZero();
    }

    @Test
    @DisplayName("Should allow retention days up to the maximum bound of 3650 days")
    void shouldAllowRetentionDaysAtMaximumBound() {
        // Arrange
        sessionRepository.deleteReturnValue = 1;

        // Act
        PurgeResult result = housekeepingService.purgeRevokedOrExpiredSessions(3650);

        // Assert
        assertThat(result.purgedCount()).isEqualTo(1);
        assertThat(result.retentionDays()).isEqualTo(3650);
        assertThat(sessionRepository.deleteCallCount).isEqualTo(1);
    }

    @Test
    @DisplayName("Should throw NullPointerException when UserSessionRepositoryPort is null")
    void shouldThrowNullPointerExceptionWhenRepositoryIsNull() {
        // Arrange & Act & Assert
        assertThatThrownBy(() -> new SessionHousekeepingService(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("UserSessionRepositoryPort cannot be null");
    }

    private static class StubUserSessionRepository implements UserSessionRepositoryPort {
        int deleteReturnValue = 0;
        int deleteCallCount = 0;
        Instant lastCutoff = null;

        @Override
        public int deleteRevokedOrExpiredOlderThan(Instant cutoff) {
            deleteCallCount++;
            lastCutoff = cutoff;
            return deleteReturnValue;
        }

        @Override
        public UserSession save(UserSession session) {
            return session;
        }

        @Override
        public Optional<UserSession> findById(UUID id) {
            return Optional.empty();
        }

        @Override
        public Optional<UserSession> findByRefreshTokenHash(String refreshTokenHash) {
            return Optional.empty();
        }

        @Override
        public Optional<UserSession> findByPreviousRefreshTokenHash(String previousRefreshTokenHash) {
            return Optional.empty();
        }

        @Override
        public Optional<UserSession> findByCurrentOrPreviousTokenHash(String tokenHash) {
            return Optional.empty();
        }

        @Override
        public List<UserSession> findActiveByAccountId(UUID accountId) {
            return List.of();
        }

        @Override
        public void revokeAllForAccount(UUID accountId, String reason, Instant revokedAt) {
        }

        @Override
        public void revokeAllExcept(UUID accountId, UUID excludedSessionId, String reason, Instant revokedAt) {
        }
    }
}
