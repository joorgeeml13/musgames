package dev.ordago.system.infrastructure.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import javax.sql.DataSource;
import jakarta.inject.Inject;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import dev.ordago.auth.domain.port.in.PurgeExpiredSessionsUseCase;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class HousekeepingResourceIT {

    private static final String HOUSEKEEPING_HEADER = "X-Housekeeping-Key";
    private static final String VALID_KEY = "ordago-dev-housekeeping-secret-key-32chars";
    private static final String WRONG_KEY = "wrong-housekeeping-secret-key-here";
    private static final String PURGE_ENDPOINT = "/api/v1/system/housekeeping/sessions/purge";

    @Inject
    DataSource dataSource;

    private UUID testAccountId;

    @BeforeEach
    void setUp() throws SQLException {
        testAccountId = UUID.randomUUID();
        insertAccount(testAccountId, "housekeeping-" + testAccountId + "@ordago.dev", "hash", Instant.now());
    }

    @AfterEach
    void tearDown() {
        if (testAccountId != null) {
            deleteAccount(testAccountId);
        }
    }

    @Test
    @DisplayName("Should return 401 when X-Housekeeping-Key header is missing")
    void shouldReturn401WhenHousekeepingKeyIsMissing() {
        // Arrange, Act & Assert
        given()
                .contentType(ContentType.JSON)
                .when()
                .post(PURGE_ENDPOINT)
                .then()
                .statusCode(401)
                .body("code", equalTo("UNAUTHORIZED"))
                .body("status", equalTo(401))
                .body("message", equalTo("Invalid or missing housekeeping authorization key"));
    }

    @Test
    @DisplayName("Should return 401 when X-Housekeeping-Key header is empty")
    void shouldReturn401WhenHousekeepingKeyIsEmpty() {
        // Arrange, Act & Assert
        given()
                .header(HOUSEKEEPING_HEADER, "")
                .contentType(ContentType.JSON)
                .when()
                .post(PURGE_ENDPOINT)
                .then()
                .statusCode(401)
                .body("code", equalTo("UNAUTHORIZED"))
                .body("status", equalTo(401))
                .body("message", equalTo("Invalid or missing housekeeping authorization key"));
    }

    @Test
    @DisplayName("Should return 401 when X-Housekeeping-Key header is blank or whitespace")
    void shouldReturn401WhenHousekeepingKeyIsBlank() {
        // Arrange, Act & Assert
        given()
                .header(HOUSEKEEPING_HEADER, "   ")
                .contentType(ContentType.JSON)
                .when()
                .post(PURGE_ENDPOINT)
                .then()
                .statusCode(401)
                .body("code", equalTo("UNAUTHORIZED"))
                .body("status", equalTo(401))
                .body("message", equalTo("Invalid or missing housekeeping authorization key"));
    }

    @Test
    @DisplayName("Should return 401 when X-Housekeeping-Key header is invalid")
    void shouldReturn401WhenHousekeepingKeyIsInvalid() {
        // Arrange, Act & Assert
        given()
                .header(HOUSEKEEPING_HEADER, WRONG_KEY)
                .contentType(ContentType.JSON)
                .when()
                .post(PURGE_ENDPOINT)
                .then()
                .statusCode(401)
                .body("code", equalTo("UNAUTHORIZED"))
                .body("status", equalTo(401))
                .body("message", equalTo("Invalid or missing housekeeping authorization key"));
    }

    @Test
    @DisplayName("Should return 400 when retentionDays query param is negative")
    void shouldReturn400WhenRetentionDaysIsNegative() {
        // Arrange, Act & Assert
        given()
                .header(HOUSEKEEPING_HEADER, VALID_KEY)
                .queryParam("retentionDays", -5)
                .contentType(ContentType.JSON)
                .when()
                .post(PURGE_ENDPOINT)
                .then()
                .statusCode(400)
                .body("code", equalTo("VALIDATION_ERROR"))
                .body("status", equalTo(400));
    }

    @Test
    @DisplayName("Should return 400 when retentionDays query param exceeds maximum allowed (3650)")
    void shouldReturn400WhenRetentionDaysExceedsMaximum() {
        given()
                .header(HOUSEKEEPING_HEADER, VALID_KEY)
                .queryParam("retentionDays", 3651)
                .contentType(ContentType.JSON)
                .when()
                .post(PURGE_ENDPOINT)
                .then()
                .statusCode(400)
                .body("code", equalTo("VALIDATION_ERROR"))
                .body("status", equalTo(400))
                .body("message", containsString("Retention days must be between 0 and 3650"));
    }

    @Test
    @DisplayName("Should enforce fail-fast constructor validation when api-key is null, blank, or shorter than 32 chars")
    void shouldEnforceConstructorValidationOnApiKey() {
        PurgeExpiredSessionsUseCase mockUseCase = retentionDays -> null;

        assertThatThrownBy(() -> new HousekeepingResource(null, VALID_KEY))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> new HousekeepingResource(mockUseCase, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ordago.housekeeping.api-key must not be blank and must be at least 32 characters long");

        assertThatThrownBy(() -> new HousekeepingResource(mockUseCase, "   "))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ordago.housekeeping.api-key must not be blank and must be at least 32 characters long");

        assertThatThrownBy(() -> new HousekeepingResource(mockUseCase, "short-key-16char"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ordago.housekeeping.api-key must not be blank and must be at least 32 characters long");
    }

    @Test
    @DisplayName("Should return 200 with default retention days when valid key is provided")
    void shouldReturn200WithDefaultRetentionDays() {
        // Arrange, Act & Assert
        given()
                .header(HOUSEKEEPING_HEADER, VALID_KEY)
                .contentType(ContentType.JSON)
                .when()
                .post(PURGE_ENDPOINT)
                .then()
                .statusCode(200)
                .body("retentionDays", equalTo(30))
                .body("purgedCount", greaterThanOrEqualTo(0))
                .body("cutoff", notNullValue())
                .body("executionTimeMs", greaterThanOrEqualTo(0));
    }

    @Test
    @DisplayName("Should purge all expired and revoked sessions when retentionDays is 0 while retaining active sessions")
    void shouldPurgeExpiredAndRevokedSessionsWhenRetentionDaysIsZero() throws SQLException {
        // Arrange
        Instant now = Instant.now();

        // 1. Active session (valid for 7 days, not revoked)
        UUID activeSessionId = UUID.randomUUID();
        insertSession(
                activeSessionId,
                testAccountId,
                "device-active-zero",
                "hash-active-zero-" + activeSessionId,
                now.plus(Duration.ofDays(7)),
                false,
                null,
                null,
                now.minus(Duration.ofDays(10)),
                now.minus(Duration.ofDays(1))
        );

        // 2. Old active session created 60 days ago (refreshed, valid for 5 days, not revoked)
        UUID oldActiveSessionId = UUID.randomUUID();
        insertSession(
                oldActiveSessionId,
                testAccountId,
                "device-old-active-zero",
                "hash-old-active-" + oldActiveSessionId,
                now.plus(Duration.ofDays(5)),
                false,
                null,
                null,
                now.minus(Duration.ofDays(60)),
                now.minus(Duration.ofDays(1))
        );

        // 3. Expired session (expired 1 hour ago)
        UUID expiredSessionId = UUID.randomUUID();
        insertSession(
                expiredSessionId,
                testAccountId,
                "device-expired-zero",
                "hash-exp-zero-" + expiredSessionId,
                now.minus(Duration.ofHours(1)),
                false,
                null,
                null,
                now.minus(Duration.ofDays(30)),
                now.minus(Duration.ofHours(1))
        );

        // 4. Revoked session (revoked 1 hour ago)
        UUID revokedSessionId = UUID.randomUUID();
        insertSession(
                revokedSessionId,
                testAccountId,
                "device-revoked-zero",
                "hash-rev-zero-" + revokedSessionId,
                now.plus(Duration.ofDays(7)),
                true,
                now.minus(Duration.ofHours(1)),
                "USER_LOGOUT",
                now.minus(Duration.ofDays(10)),
                now.minus(Duration.ofHours(1))
        );

        assertThat(sessionExists(activeSessionId)).isTrue();
        assertThat(sessionExists(oldActiveSessionId)).isTrue();
        assertThat(sessionExists(expiredSessionId)).isTrue();
        assertThat(sessionExists(revokedSessionId)).isTrue();

        // Act
        given()
                .header(HOUSEKEEPING_HEADER, VALID_KEY)
                .queryParam("retentionDays", 0)
                .contentType(ContentType.JSON)
                .when()
                .post(PURGE_ENDPOINT)
                .then()
                .statusCode(200)
                .body("retentionDays", equalTo(0))
                .body("purgedCount", greaterThanOrEqualTo(2));

        // Assert
        assertThat(sessionExists(activeSessionId))
                .as("Active session must remain intact when retentionDays=0")
                .isTrue();

        assertThat(sessionExists(oldActiveSessionId))
                .as("Active session created 60 days ago must remain intact when retentionDays=0")
                .isTrue();

        assertThat(sessionExists(expiredSessionId))
                .as("Expired session must be purged when retentionDays=0")
                .isFalse();

        assertThat(sessionExists(revokedSessionId))
                .as("Revoked session must be purged when retentionDays=0")
                .isFalse();
    }

    @Test
    @DisplayName("Should purge ONLY expired/revoked >30d sessions while keeping active and recent sessions intact")
    void shouldPurgeOnlyOldRevokedOrExpiredSessionsAndRetainActiveAndRecent() throws SQLException {
        // Arrange
        Instant now = Instant.now();

        // 1. Active session (valid for 7 more days, created 2 days ago, not revoked)
        UUID activeSessionId = UUID.randomUUID();
        insertSession(
                activeSessionId,
                testAccountId,
                "device-active",
                "hash-active-" + activeSessionId,
                now.plus(Duration.ofDays(7)),
                false,
                null,
                null,
                now.minus(Duration.ofDays(2)),
                now.minus(Duration.ofDays(1))
        );

        // 2. Active session created 60 days ago (refreshed, valid for 7 more days, not revoked)
        UUID oldActiveSessionId = UUID.randomUUID();
        insertSession(
                oldActiveSessionId,
                testAccountId,
                "device-old-active-30d",
                "hash-old-active-30d-" + oldActiveSessionId,
                now.plus(Duration.ofDays(7)),
                false,
                null,
                null,
                now.minus(Duration.ofDays(60)),
                now.minus(Duration.ofDays(1))
        );

        // 3. Recently revoked session (<30d, revoked 5 days ago)
        UUID recentRevokedSessionId = UUID.randomUUID();
        insertSession(
                recentRevokedSessionId,
                testAccountId,
                "device-recent-revoked",
                "hash-recent-" + recentRevokedSessionId,
                now.plus(Duration.ofDays(10)),
                true,
                now.minus(Duration.ofDays(5)),
                "USER_LOGOUT",
                now.minus(Duration.ofDays(10)),
                now.minus(Duration.ofDays(5))
        );

        // 4. Recently revoked session (<30d, revokedAt is null, created 5 days ago)
        UUID recentRevokedNullRevokedAtId = UUID.randomUUID();
        insertSession(
                recentRevokedNullRevokedAtId,
                testAccountId,
                "device-recent-revoked-null",
                "hash-recent-null-" + recentRevokedNullRevokedAtId,
                now.plus(Duration.ofDays(10)),
                true,
                null,
                "LEGACY_LOGOUT",
                now.minus(Duration.ofDays(5)),
                now.minus(Duration.ofDays(5))
        );

        // 5. Expired session (>30d, expired 35 days ago, not revoked)
        UUID expiredOldSessionId = UUID.randomUUID();
        insertSession(
                expiredOldSessionId,
                testAccountId,
                "device-expired-old",
                "hash-exp-" + expiredOldSessionId,
                now.minus(Duration.ofDays(35)),
                false,
                null,
                null,
                now.minus(Duration.ofDays(65)),
                now.minus(Duration.ofDays(35))
        );

        // 6. Revoked session (>30d, revoked 35 days ago)
        UUID revokedOldSessionId = UUID.randomUUID();
        insertSession(
                revokedOldSessionId,
                testAccountId,
                "device-revoked-old",
                "hash-rev-" + revokedOldSessionId,
                now.plus(Duration.ofDays(10)),
                true,
                now.minus(Duration.ofDays(35)),
                "SECURITY_PURGE",
                now.minus(Duration.ofDays(60)),
                now.minus(Duration.ofDays(35))
        );

        // 7. Revoked session (>30d, revokedAt is null, created 35 days ago)
        UUID oldRevokedNullRevokedAtId = UUID.randomUUID();
        insertSession(
                oldRevokedNullRevokedAtId,
                testAccountId,
                "device-old-revoked-null",
                "hash-old-null-" + oldRevokedNullRevokedAtId,
                now.plus(Duration.ofDays(10)),
                true,
                null,
                "OLD_LEGACY_LOGOUT",
                now.minus(Duration.ofDays(35)),
                now.minus(Duration.ofDays(35))
        );

        // Verify initial state: all 7 exist
        assertThat(sessionExists(activeSessionId)).isTrue();
        assertThat(sessionExists(oldActiveSessionId)).isTrue();
        assertThat(sessionExists(recentRevokedSessionId)).isTrue();
        assertThat(sessionExists(recentRevokedNullRevokedAtId)).isTrue();
        assertThat(sessionExists(expiredOldSessionId)).isTrue();
        assertThat(sessionExists(revokedOldSessionId)).isTrue();
        assertThat(sessionExists(oldRevokedNullRevokedAtId)).isTrue();

        // Act
        given()
                .header(HOUSEKEEPING_HEADER, VALID_KEY)
                .queryParam("retentionDays", 30)
                .contentType(ContentType.JSON)
                .when()
                .post(PURGE_ENDPOINT)
                .then()
                .statusCode(200)
                .body("retentionDays", equalTo(30))
                .body("purgedCount", greaterThanOrEqualTo(3));

        // Assert
        // Active and recently revoked sessions must still exist
        assertThat(sessionExists(activeSessionId))
                .as("Active session must remain intact after housekeeping purge")
                .isTrue();

        assertThat(sessionExists(oldActiveSessionId))
                .as("Active session created 60 days ago must remain intact after housekeeping purge")
                .isTrue();

        assertThat(sessionExists(recentRevokedSessionId))
                .as("Recently revoked session (<30d) must remain intact after housekeeping purge")
                .isTrue();

        assertThat(sessionExists(recentRevokedNullRevokedAtId))
                .as("Recently revoked session with null revoked_at (<30d) must remain intact")
                .isTrue();

        // Expired >30d and Revoked >30d sessions must have been deleted
        assertThat(sessionExists(expiredOldSessionId))
                .as("Expired session (>30d) must be purged")
                .isFalse();

        assertThat(sessionExists(revokedOldSessionId))
                .as("Revoked session (>30d) must be purged")
                .isFalse();

        assertThat(sessionExists(oldRevokedNullRevokedAtId))
                .as("Revoked session with null revoked_at (>30d) must be purged")
                .isFalse();
    }

    private void insertAccount(UUID id, String email, String passwordHash, Instant timestamp) throws SQLException {
        String sql = """
                INSERT INTO accounts (id, email, password_hash, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            ps.setString(2, email);
            ps.setString(3, passwordHash);
            ps.setTimestamp(4, Timestamp.from(timestamp));
            ps.setTimestamp(5, Timestamp.from(timestamp));
            ps.executeUpdate();
        }
    }

    private void insertSession(
            UUID sessionId,
            UUID accountId,
            String deviceId,
            String tokenHash,
            Instant expiresAt,
            boolean isRevoked,
            Instant revokedAt,
            String revocationReason,
            Instant createdAt,
            Instant lastRefreshedAt
    ) throws SQLException {
        String sql = """
                INSERT INTO user_sessions (
                    id, account_id, device_id, device_name,
                    refresh_token_hash, expires_at,
                    is_revoked, revoked_at, revocation_reason,
                    created_at, last_refreshed_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, sessionId);
            ps.setObject(2, accountId);
            ps.setString(3, deviceId);
            ps.setString(4, "TestDevice");
            ps.setString(5, tokenHash);
            ps.setTimestamp(6, Timestamp.from(expiresAt));
            ps.setBoolean(7, isRevoked);
            ps.setTimestamp(8, revokedAt != null ? Timestamp.from(revokedAt) : null);
            ps.setString(9, revocationReason);
            ps.setTimestamp(10, Timestamp.from(createdAt));
            ps.setTimestamp(11, Timestamp.from(lastRefreshedAt));
            ps.executeUpdate();
        }
    }

    private boolean sessionExists(UUID id) throws SQLException {
        String sql = "SELECT COUNT(*) FROM user_sessions WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private void deleteAccount(UUID id) {
        String sql = "DELETE FROM accounts WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            ps.executeUpdate();
        } catch (SQLException ignored) {
        }
    }
}
