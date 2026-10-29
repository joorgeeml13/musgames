package dev.ordago.auth.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import dev.ordago.auth.domain.model.UserSession;

@Entity
@Table(
        name = "user_sessions",
        indexes = {
                @Index(name = "idx_user_sessions_account_id", columnList = "account_id"),
                @Index(name = "idx_user_sessions_refresh_hash", columnList = "refresh_token_hash", unique = true),
                @Index(name = "idx_user_sessions_prev_refresh_hash", columnList = "previous_refresh_token_hash")
        }
)
public class UserSessionEntity extends PanacheEntityBase {

    @Id
    @Column(name = "id", nullable = false)
    public UUID id;

    @Column(name = "account_id", nullable = false)
    public UUID accountId;

    @Column(name = "device_id", nullable = false, length = 128)
    public String deviceId;

    @Column(name = "device_name", length = 128)
    public String deviceName;

    @Column(name = "refresh_token_hash", nullable = false, unique = true, length = 255)
    public String refreshTokenHash;

    @Column(name = "previous_refresh_token_hash", length = 255)
    public String previousRefreshTokenHash;

    @Column(name = "expires_at", nullable = false)
    public Instant expiresAt;

    @Column(name = "is_revoked", nullable = false)
    public boolean isRevoked = false;

    @Column(name = "revoked_at")
    public Instant revokedAt;

    @Column(name = "revocation_reason", length = 64)
    public String revocationReason;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "last_refreshed_at", nullable = false)
    public Instant lastRefreshedAt;

    public UserSession toDomain() {
        return new UserSession(
                this.id,
                this.accountId,
                this.deviceId,
                this.deviceName,
                this.refreshTokenHash,
                this.previousRefreshTokenHash,
                this.expiresAt,
                this.isRevoked,
                this.revokedAt,
                this.revocationReason,
                this.createdAt,
                this.lastRefreshedAt
        );
    }

    public static UserSessionEntity fromDomain(UserSession domain) {
        UserSessionEntity entity = new UserSessionEntity();
        entity.id = domain.getId();
        entity.accountId = domain.getAccountId();
        entity.deviceId = domain.getDeviceId();
        entity.deviceName = domain.getDeviceName();
        entity.refreshTokenHash = domain.getRefreshTokenHash();
        entity.previousRefreshTokenHash = domain.getPreviousRefreshTokenHash();
        entity.expiresAt = domain.getExpiresAt();
        entity.isRevoked = domain.isRevoked();
        entity.revokedAt = domain.getRevokedAt();
        entity.revocationReason = domain.getRevocationReason();
        entity.createdAt = domain.getCreatedAt();
        entity.lastRefreshedAt = domain.getLastRefreshedAt();
        return entity;
    }
}
