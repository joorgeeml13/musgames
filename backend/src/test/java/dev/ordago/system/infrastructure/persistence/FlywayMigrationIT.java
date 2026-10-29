package dev.ordago.system.infrastructure.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import javax.sql.DataSource;
import jakarta.inject.Inject;
import io.quarkus.test.junit.QuarkusTest;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@QuarkusTest
class FlywayMigrationIT {

    @Inject
    Flyway flyway;

    @Inject
    DataSource dataSource;

    @Test
    @DisplayName("Flyway migrations should be applied successfully with version 1.0.0")
    void shouldApplyFlywayMigrationsSuccessfully() {
        // Arrange & Act
        MigrationInfo current = flyway.info().current();
        MigrationInfo[] applied = flyway.info().applied();

        // Assert
        assertThat(current)
                .as("Current migration info should not be null")
                .isNotNull();
        assertThat(current.getVersion())
                .as("Current migration version should not be null")
                .isNotNull();
        assertThat(current.getVersion().getVersion())
                .as("Current migration version must be 1.0.0")
                .isEqualTo("1.0.0");
        assertThat(current.getState())
                .as("Current migration state should be SUCCESS")
                .isEqualTo(MigrationState.SUCCESS);

        assertThat(applied)
                .as("Applied migrations array should contain exactly 1 migration")
                .hasSize(1);
        assertThat(applied[0].getVersion().getVersion())
                .as("Applied migration version should be 1.0.0")
                .isEqualTo("1.0.0");
        assertThat(applied[0].getState())
                .as("Applied migration state should be SUCCESS")
                .isEqualTo(MigrationState.SUCCESS);
    }

    @Test
    @DisplayName("Database should enforce UNIQUE constraint on accounts(email)")
    void shouldEnforceUniqueEmailConstraintOnAccounts() throws SQLException {
        // Arrange
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        String duplicateEmail = "duplicate-" + UUID.randomUUID() + "@ordago.dev";
        Instant now = Instant.now();

        try {
            insertAccount(id1, duplicateEmail, "hash1", now);

            // Act & Assert
            assertThatThrownBy(() -> insertAccount(id2, duplicateEmail, "hash2", now))
                    .as("Inserting account with duplicate email should violate unique constraint")
                    .isInstanceOf(SQLException.class);
        } finally {
            deleteAccount(id1);
            deleteAccount(id2);
        }
    }

    @Test
    @DisplayName("Database should enforce case-insensitive UNIQUE constraint on user_profiles(LOWER(game_name), LOWER(tag_line))")
    void shouldEnforceCaseInsensitiveUniqueRiotIdConstraintOnUserProfiles() throws SQLException {
        // Arrange
        UUID accountId1 = UUID.randomUUID();
        UUID accountId2 = UUID.randomUUID();
        Instant now = Instant.now();
        String gameNameUpper = "MusMaster" + UUID.randomUUID().toString().substring(0, 6);
        String gameNameLower = gameNameUpper.toLowerCase();
        String tagLineUpper = "TAG1";
        String tagLineLower = "tag1";

        try {
            insertAccount(accountId1, "p1-" + UUID.randomUUID() + "@ordago.dev", "hash1", now);
            insertAccount(accountId2, "p2-" + UUID.randomUUID() + "@ordago.dev", "hash2", now);

            insertUserProfile(accountId1, gameNameUpper, tagLineUpper, now);

            // Act & Assert: try inserting with different casing for game_name and tag_line
            assertThatThrownBy(() -> insertUserProfile(accountId2, gameNameLower, tagLineLower, now))
                    .as("Inserting profile with duplicate case-insensitive Riot ID should violate unique constraint")
                    .isInstanceOf(SQLException.class);
        } finally {
            deleteAccount(accountId1);
            deleteAccount(accountId2);
        }
    }

    @Test
    @DisplayName("Database should enforce FOREIGN KEY constraint on user_profiles(id) referencing accounts(id)")
    void shouldEnforceForeignKeyConstraintOnUserProfiles() {
        // Arrange
        UUID nonExistentAccountId = UUID.randomUUID();
        Instant now = Instant.now();

        // Act & Assert
        assertThatThrownBy(() -> insertUserProfile(nonExistentAccountId, "OrphanPlayer", "NA1", now))
                .as("Inserting user profile for non-existent account should violate foreign key constraint")
                .isInstanceOf(SQLException.class);
    }

    @Test
    @DisplayName("Database should cascade DELETE from accounts to user_profiles and user_sessions")
    void shouldCascadeDeleteFromAccountToProfilesAndSessions() throws SQLException {
        // Arrange
        UUID accountId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        Instant now = Instant.now();
        String email = "cascade-" + UUID.randomUUID() + "@ordago.dev";

        try {
            insertAccount(accountId, email, "passhash", now);
            insertUserProfile(accountId, "CascadeUser", "CAS1", now);
            insertUserSession(sessionId, accountId, "device-cas-1", "hash-" + UUID.randomUUID(), now);

            assertThat(countRows("SELECT COUNT(*) FROM user_profiles WHERE id = ?", accountId)).isEqualTo(1);
            assertThat(countRows("SELECT COUNT(*) FROM user_sessions WHERE id = ?", sessionId)).isEqualTo(1);

            // Act: delete the parent account
            deleteAccount(accountId);

            // Assert: child records in user_profiles and user_sessions must be cascade-deleted
            assertThat(countRows("SELECT COUNT(*) FROM user_profiles WHERE id = ?", accountId)).isEqualTo(0);
            assertThat(countRows("SELECT COUNT(*) FROM user_sessions WHERE id = ?", sessionId)).isEqualTo(0);
        } finally {
            deleteAccount(accountId);
        }
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

    private void insertUserProfile(UUID id, String gameName, String tagLine, Instant timestamp) throws SQLException {
        String sql = """
                INSERT INTO user_profiles (id, game_name, tag_line, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            ps.setString(2, gameName);
            ps.setString(3, tagLine);
            ps.setTimestamp(4, Timestamp.from(timestamp));
            ps.setTimestamp(5, Timestamp.from(timestamp));
            ps.executeUpdate();
        }
    }

    private void insertUserSession(UUID id, UUID accountId, String deviceId, String refreshTokenHash, Instant timestamp) throws SQLException {
        String sql = """
                INSERT INTO user_sessions (id, account_id, device_id, refresh_token_hash, expires_at, created_at, last_refreshed_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            ps.setObject(2, accountId);
            ps.setString(3, deviceId);
            ps.setString(4, refreshTokenHash);
            ps.setTimestamp(5, Timestamp.from(timestamp.plusSeconds(3600)));
            ps.setTimestamp(6, Timestamp.from(timestamp));
            ps.setTimestamp(7, Timestamp.from(timestamp));
            ps.executeUpdate();
        }
    }

    private void deleteAccount(UUID id) {
        String sql = "DELETE FROM accounts WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            ps.executeUpdate();
        } catch (SQLException ignored) {
            // best-effort cleanup
        }
    }

    private int countRows(String sql, UUID id) throws SQLException {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }
}
