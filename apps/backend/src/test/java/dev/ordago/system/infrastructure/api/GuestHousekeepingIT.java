package dev.ordago.system.infrastructure.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
@DisplayName("Guest Accounts Housekeeping & Purge Integration Tests")
class GuestHousekeepingIT {

    private static final String HOUSEKEEPING_HEADER = "X-Housekeeping-Key";
    private static final String VALID_KEY = "ordago-dev-housekeeping-secret-key-32chars";
    private static final String WRONG_KEY = "wrong-housekeeping-secret-key-here";
    private static final String PURGE_GUESTS_ENDPOINT = "/api/v1/system/housekeeping/guests/purge";

    @Inject
    DataSource dataSource;

    private final List<UUID> createdAccountIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        createdAccountIds.clear();
    }

    @AfterEach
    void tearDown() {
        for (UUID accountId : createdAccountIds) {
            deleteAccountIfExists(accountId);
        }
        createdAccountIds.clear();
    }

    // =========================================================================
    // 1. Authorization checks for X-Housekeeping-Key
    // =========================================================================

    @Test
    @DisplayName("Should return 401 Unauthorized when X-Housekeeping-Key header is missing")
    void shouldReturn401WhenHousekeepingKeyIsMissing() {
        // Arrange, Act & Assert
        given()
                .contentType(ContentType.JSON)
                .when()
                .post(PURGE_GUESTS_ENDPOINT)
                .then()
                .statusCode(401)
                .body("code", equalTo("UNAUTHORIZED"))
                .body("status", equalTo(401))
                .body("message", equalTo("Invalid or missing housekeeping authorization key"));
    }

    @Test
    @DisplayName("Should return 401 Unauthorized when X-Housekeeping-Key header is empty")
    void shouldReturn401WhenHousekeepingKeyIsEmpty() {
        // Arrange, Act & Assert
        given()
                .header(HOUSEKEEPING_HEADER, "")
                .contentType(ContentType.JSON)
                .when()
                .post(PURGE_GUESTS_ENDPOINT)
                .then()
                .statusCode(401)
                .body("code", equalTo("UNAUTHORIZED"))
                .body("status", equalTo(401))
                .body("message", equalTo("Invalid or missing housekeeping authorization key"));
    }

    @Test
    @DisplayName("Should return 401 Unauthorized when X-Housekeeping-Key header is blank whitespace")
    void shouldReturn401WhenHousekeepingKeyIsBlank() {
        // Arrange, Act & Assert
        given()
                .header(HOUSEKEEPING_HEADER, "   ")
                .contentType(ContentType.JSON)
                .when()
                .post(PURGE_GUESTS_ENDPOINT)
                .then()
                .statusCode(401)
                .body("code", equalTo("UNAUTHORIZED"))
                .body("status", equalTo(401))
                .body("message", equalTo("Invalid or missing housekeeping authorization key"));
    }

    @Test
    @DisplayName("Should return 401 Unauthorized when X-Housekeeping-Key header is invalid")
    void shouldReturn401WhenHousekeepingKeyIsInvalid() {
        // Arrange, Act & Assert
        given()
                .header(HOUSEKEEPING_HEADER, WRONG_KEY)
                .contentType(ContentType.JSON)
                .when()
                .post(PURGE_GUESTS_ENDPOINT)
                .then()
                .statusCode(401)
                .body("code", equalTo("UNAUTHORIZED"))
                .body("status", equalTo(401))
                .body("message", equalTo("Invalid or missing housekeeping authorization key"));
    }

    // =========================================================================
    // 2. Query param validation
    // =========================================================================

    @Test
    @DisplayName("Should return 400 Bad Request when retentionDays query param is negative")
    void shouldReturn400WhenRetentionDaysIsNegative() {
        // Arrange, Act & Assert
        given()
                .header(HOUSEKEEPING_HEADER, VALID_KEY)
                .queryParam("retentionDays", -1)
                .contentType(ContentType.JSON)
                .when()
                .post(PURGE_GUESTS_ENDPOINT)
                .then()
                .statusCode(400)
                .body("code", equalTo("VALIDATION_ERROR"))
                .body("status", equalTo(400));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when retentionDays query param exceeds 3650")
    void shouldReturn400WhenRetentionDaysExceedsMaximum() {
        // Arrange, Act & Assert
        given()
                .header(HOUSEKEEPING_HEADER, VALID_KEY)
                .queryParam("retentionDays", 3651)
                .contentType(ContentType.JSON)
                .when()
                .post(PURGE_GUESTS_ENDPOINT)
                .then()
                .statusCode(400)
                .body("code", equalTo("VALIDATION_ERROR"))
                .body("status", equalTo(400))
                .body("message", containsString("Retention days must be between 0 and 3650"));
    }

    @Test
    @DisplayName("Should return 200 OK with default retention days (7) when valid key is provided")
    void shouldReturn200WithDefaultRetentionDaysWhenValidKeyProvided() {
        // Arrange, Act & Assert
        given()
                .header(HOUSEKEEPING_HEADER, VALID_KEY)
                .contentType(ContentType.JSON)
                .when()
                .post(PURGE_GUESTS_ENDPOINT)
                .then()
                .statusCode(200)
                .body("retentionDays", equalTo(7))
                .body("purgedCount", greaterThanOrEqualTo(0))
                .body("cutoff", notNullValue())
                .body("executionTimeMs", greaterThanOrEqualTo(0));
    }

    // =========================================================================
    // 3. Purge behavior and ON DELETE CASCADE verification
    // =========================================================================

    @Test
    @DisplayName("Should purge guest accounts older than retentionDays and CASCADE delete their profiles and sessions")
    void shouldPurgeExpiredGuestAccountsAndCascadeDeleteProfilesAndSessions() throws SQLException {
        // Arrange
        Instant now = Instant.now();

        // 1. Expired guest account (created 10 days ago, is_guest=true)
        UUID expiredGuestAccountId = UUID.randomUUID();
        UUID expiredGuestSessionId = UUID.randomUUID();
        String expiredGuestTagSuffix = expiredGuestAccountId.toString().substring(0, 4);
        insertAccount(expiredGuestAccountId, null, null, true, now.minus(Duration.ofDays(10)));
        insertProfile(expiredGuestAccountId, "GstExp" + expiredGuestTagSuffix, "666", now.minus(Duration.ofDays(10)));
        insertSession(expiredGuestSessionId, expiredGuestAccountId, "dev-exp-guest", "token-hash-exp-1", now.plus(Duration.ofDays(20)), now.minus(Duration.ofDays(10)));
        createdAccountIds.add(expiredGuestAccountId);

        // 2. Recent guest account (created 2 days ago, is_guest=true)
        UUID recentGuestAccountId = UUID.randomUUID();
        UUID recentGuestSessionId = UUID.randomUUID();
        String recentGuestTagSuffix = recentGuestAccountId.toString().substring(0, 4);
        insertAccount(recentGuestAccountId, null, null, true, now.minus(Duration.ofDays(2)));
        insertProfile(recentGuestAccountId, "GstRcn" + recentGuestTagSuffix, "666", now.minus(Duration.ofDays(2)));
        insertSession(recentGuestSessionId, recentGuestAccountId, "dev-rcn-guest", "token-hash-rcn-1", now.plus(Duration.ofDays(28)), now.minus(Duration.ofDays(2)));
        createdAccountIds.add(recentGuestAccountId);

        // 3. Old regular account (created 20 days ago, is_guest=false) -> MUST NOT be purged
        UUID oldRegularAccountId = UUID.randomUUID();
        UUID oldRegularSessionId = UUID.randomUUID();
        String oldRegularTagSuffix = oldRegularAccountId.toString().substring(0, 4);
        insertAccount(oldRegularAccountId, "regular_old_" + oldRegularTagSuffix + "@ordago.dev", "regular_hash", false, now.minus(Duration.ofDays(20)));
        insertProfile(oldRegularAccountId, "RegOld" + oldRegularTagSuffix, "ESP1", now.minus(Duration.ofDays(20)));
        insertSession(oldRegularSessionId, oldRegularAccountId, "dev-reg-old", "token-hash-reg-1", now.plus(Duration.ofDays(10)), now.minus(Duration.ofDays(20)));
        createdAccountIds.add(oldRegularAccountId);

        // Precondition checks: verify all 3 accounts, profiles, and sessions exist
        assertThat(accountExists(expiredGuestAccountId)).isTrue();
        assertThat(profileExists(expiredGuestAccountId)).isTrue();
        assertThat(sessionExists(expiredGuestSessionId)).isTrue();

        assertThat(accountExists(recentGuestAccountId)).isTrue();
        assertThat(profileExists(recentGuestAccountId)).isTrue();
        assertThat(sessionExists(recentGuestSessionId)).isTrue();

        assertThat(accountExists(oldRegularAccountId)).isTrue();
        assertThat(profileExists(oldRegularAccountId)).isTrue();
        assertThat(sessionExists(oldRegularSessionId)).isTrue();

        // Act: purge guests older than 7 days
        given()
                .header(HOUSEKEEPING_HEADER, VALID_KEY)
                .queryParam("retentionDays", 7)
                .contentType(ContentType.JSON)
                .when()
                .post(PURGE_GUESTS_ENDPOINT)
                .then()
                .statusCode(200)
                .body("retentionDays", equalTo(7))
                .body("purgedCount", greaterThanOrEqualTo(1));

        // Assert 1: Expired guest account must be deleted
        assertThat(accountExists(expiredGuestAccountId))
                .as("Guest account older than retentionDays (10d > 7d) must be purged")
                .isFalse();

        // Assert 2: Cascade deletion must have removed the expired guest's profile and sessions
        assertThat(profileExists(expiredGuestAccountId))
                .as("Guest profile must be cascaded (ON DELETE CASCADE) when guest account is deleted")
                .isFalse();
        assertThat(sessionExists(expiredGuestSessionId))
                .as("Guest sessions must be cascaded (ON DELETE CASCADE) when guest account is deleted")
                .isFalse();

        // Assert 3: Recent guest account (<7d) must remain intact
        assertThat(accountExists(recentGuestAccountId))
                .as("Recent guest account (2d < 7d) must NOT be purged")
                .isTrue();
        assertThat(profileExists(recentGuestAccountId))
                .as("Recent guest profile must remain intact")
                .isTrue();
        assertThat(sessionExists(recentGuestSessionId))
                .as("Recent guest session must remain intact")
                .isTrue();

        // Assert 4: Regular account (>7d but is_guest=false) must NEVER be purged by guest housekeeper
        assertThat(accountExists(oldRegularAccountId))
                .as("Regular non-guest account must NEVER be deleted by guest housekeeping purge")
                .isTrue();
        assertThat(profileExists(oldRegularAccountId))
                .as("Regular account profile must remain intact")
                .isTrue();
        assertThat(sessionExists(oldRegularSessionId))
                .as("Regular account session must remain intact")
                .isTrue();
    }

    @Test
    @DisplayName("Should purge all guest accounts when retentionDays=0 while retaining all regular accounts")
    void shouldPurgeAllGuestAccountsWhenRetentionDaysIsZeroWhilePreservingRegularAccounts() throws SQLException {
        // Arrange
        Instant now = Instant.now();

        UUID guestAccountId = UUID.randomUUID();
        UUID guestSessionId = UUID.randomUUID();
        String guestSuffix = guestAccountId.toString().substring(0, 4);
        insertAccount(guestAccountId, null, null, true, now);
        insertProfile(guestAccountId, "GstZero" + guestSuffix, "666", now);
        insertSession(guestSessionId, guestAccountId, "dev-guest-zero", "hash-zero-1", now.plus(Duration.ofDays(7)), now);
        createdAccountIds.add(guestAccountId);

        UUID regularAccountId = UUID.randomUUID();
        UUID regularSessionId = UUID.randomUUID();
        String regularSuffix = regularAccountId.toString().substring(0, 4);
        insertAccount(regularAccountId, "regular_zero_" + regularSuffix + "@ordago.dev", "reg_hash", false, now);
        insertProfile(regularAccountId, "RegZero" + regularSuffix, "ESP0", now);
        insertSession(regularSessionId, regularAccountId, "dev-reg-zero", "hash-reg-zero-1", now.plus(Duration.ofDays(7)), now);
        createdAccountIds.add(regularAccountId);

        assertThat(accountExists(guestAccountId)).isTrue();
        assertThat(profileExists(guestAccountId)).isTrue();
        assertThat(sessionExists(guestSessionId)).isTrue();
        assertThat(accountExists(regularAccountId)).isTrue();
        assertThat(profileExists(regularAccountId)).isTrue();
        assertThat(sessionExists(regularSessionId)).isTrue();

        // Act: purge with retentionDays=0 (cut-off is now)
        given()
                .header(HOUSEKEEPING_HEADER, VALID_KEY)
                .queryParam("retentionDays", 0)
                .contentType(ContentType.JSON)
                .when()
                .post(PURGE_GUESTS_ENDPOINT)
                .then()
                .statusCode(200)
                .body("retentionDays", equalTo(0))
                .body("purgedCount", greaterThanOrEqualTo(1));

        // Assert: Guest is completely deleted along with cascaded records
        assertThat(accountExists(guestAccountId)).isFalse();
        assertThat(profileExists(guestAccountId)).isFalse();
        assertThat(sessionExists(guestSessionId)).isFalse();

        // Regular account is unaffected
        assertThat(accountExists(regularAccountId)).isTrue();
        assertThat(profileExists(regularAccountId)).isTrue();
        assertThat(sessionExists(regularSessionId)).isTrue();
    }

    // =========================================================================
    // Database Helper Methods
    // =========================================================================

    private void insertAccount(UUID id, String email, String passwordHash, boolean isGuest, Instant createdAt) throws SQLException {
        String sql = """
                INSERT INTO accounts (id, email, password_hash, is_guest, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            ps.setString(2, email);
            ps.setString(3, passwordHash);
            ps.setBoolean(4, isGuest);
            ps.setTimestamp(5, Timestamp.from(createdAt));
            ps.setTimestamp(6, Timestamp.from(createdAt));
            ps.executeUpdate();
        }
    }

    private void insertProfile(UUID accountId, String gameName, String tagLine, Instant createdAt) throws SQLException {
        String sql = """
                INSERT INTO user_profiles (id, game_name, tag_line, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, accountId);
            ps.setString(2, gameName);
            ps.setString(3, tagLine);
            ps.setTimestamp(4, Timestamp.from(createdAt));
            ps.setTimestamp(5, Timestamp.from(createdAt));
            ps.executeUpdate();
        }
    }

    private void insertSession(
            UUID sessionId,
            UUID accountId,
            String deviceId,
            String tokenHash,
            Instant expiresAt,
            Instant createdAt
    ) throws SQLException {
        String sql = """
                INSERT INTO user_sessions (
                    id, account_id, device_id, device_name,
                    refresh_token_hash, expires_at,
                    is_revoked, created_at, last_refreshed_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, sessionId);
            ps.setObject(2, accountId);
            ps.setString(3, deviceId);
            ps.setString(4, "TestDevice");
            ps.setString(5, tokenHash);
            ps.setTimestamp(6, Timestamp.from(expiresAt));
            ps.setBoolean(7, false);
            ps.setTimestamp(8, Timestamp.from(createdAt));
            ps.setTimestamp(9, Timestamp.from(createdAt));
            ps.executeUpdate();
        }
    }

    private boolean accountExists(UUID id) throws SQLException {
        return recordExists("accounts", id);
    }

    private boolean profileExists(UUID id) throws SQLException {
        return recordExists("user_profiles", id);
    }

    private boolean sessionExists(UUID sessionId) throws SQLException {
        return recordExists("user_sessions", sessionId);
    }

    private boolean recordExists(String tableName, UUID id) throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + tableName + " WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private void deleteAccountIfExists(UUID id) {
        if (id == null) {
            return;
        }
        String sql = "DELETE FROM accounts WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            ps.executeUpdate();
        } catch (SQLException ignored) {
        }
    }
}
