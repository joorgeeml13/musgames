package dev.ordago.auth.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserSessionTest {

    @Test
    void shouldCreateValidNewSession() {
        UUID sessionId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UserSession session = UserSession.createNew(
                sessionId,
                accountId,
                "device-1",
                "Chrome Desktop",
                "hash-123",
                Duration.ofDays(30)
        );

        assertThat(session.getId()).isEqualTo(sessionId);
        assertThat(session.getAccountId()).isEqualTo(accountId);
        assertThat(session.getDeviceId()).isEqualTo("device-1");
        assertThat(session.getDeviceName()).isEqualTo("Chrome Desktop");
        assertThat(session.getRefreshTokenHash()).isEqualTo("hash-123");
        assertThat(session.getPreviousRefreshTokenHash()).isNull();
        assertThat(session.isValid()).isTrue();
        assertThat(session.isRevoked()).isFalse();
        assertThat(session.isExpired()).isFalse();
    }

    @Test
    void shouldDetectExpiredSession() {
        UUID sessionId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        Instant now = Instant.now();

        UserSession expiredSession = new UserSession(
                sessionId,
                accountId,
                "device-1",
                "Firefox",
                "hash-old",
                null,
                now.minusSeconds(10),
                false,
                null,
                null,
                now.minus(Duration.ofDays(31)),
                now.minus(Duration.ofDays(31))
        );

        assertThat(expiredSession.isExpired()).isTrue();
        assertThat(expiredSession.isValid()).isFalse();
    }

    @Test
    void shouldRotateSessionWithNewTokenHash() {
        UUID sessionId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UserSession session = UserSession.createNew(
                sessionId,
                accountId,
                "device-1",
                "Safari",
                "initial-hash",
                Duration.ofDays(30)
        );

        session.rotate("rotated-hash", Duration.ofDays(30));

        assertThat(session.getPreviousRefreshTokenHash()).isEqualTo("initial-hash");
        assertThat(session.getRefreshTokenHash()).isEqualTo("rotated-hash");
        assertThat(session.isValid()).isTrue();
    }

    @Test
    void shouldDetectGraceWindow() {
        UUID sessionId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UserSession session = UserSession.createNew(
                sessionId,
                accountId,
                "device-1",
                "Safari",
                "hash",
                Duration.ofDays(30)
        );

        // Immediately after creation, grace window is active
        assertThat(session.isWithinGraceWindow(30)).isTrue();

        // If lastRefreshedAt was 35 seconds ago, grace window of 30s has expired
        session.setLastRefreshedAt(Instant.now().minusSeconds(35));
        assertThat(session.isWithinGraceWindow(30)).isFalse();
    }

    @Test
    void shouldRevokeSessionWithReason() {
        UUID sessionId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UserSession session = UserSession.createNew(
                sessionId,
                accountId,
                "device-1",
                "Edge",
                "hash",
                Duration.ofDays(30)
        );

        session.revoke("USER_LOGOUT");

        assertThat(session.isRevoked()).isTrue();
        assertThat(session.getRevocationReason()).isEqualTo("USER_LOGOUT");
        assertThat(session.getRevokedAt()).isNotNull();
        assertThat(session.isValid()).isFalse();
    }

    @Test
    void shouldRejectNullMandatoryFields() {
        UUID id = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();

        assertThatThrownBy(() -> UserSession.createNew(null, accountId, "dev", "name", "hash", Duration.ofDays(1)))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> UserSession.createNew(id, null, "dev", "name", "hash", Duration.ofDays(1)))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> UserSession.createNew(id, accountId, null, "name", "hash", Duration.ofDays(1)))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> UserSession.createNew(id, accountId, "dev", "name", null, Duration.ofDays(1)))
                .isInstanceOf(NullPointerException.class);
    }
}
