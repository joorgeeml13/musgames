package dev.ordago.auth.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class UserSession {

    private final UUID id;
    private final UUID accountId;
    private final String deviceId;
    private final String deviceName;
    private String refreshTokenHash;
    private String previousRefreshTokenHash;
    private Instant expiresAt;
    private boolean isRevoked;
    private Instant revokedAt;
    private String revocationReason;
    private final Instant createdAt;
    private Instant lastRefreshedAt;

    public UserSession(
            UUID id,
            UUID accountId,
            String deviceId,
            String deviceName,
            String refreshTokenHash,
            String previousRefreshTokenHash,
            Instant expiresAt,
            boolean isRevoked,
            Instant revokedAt,
            String revocationReason,
            Instant createdAt,
            Instant lastRefreshedAt
    ) {
        this.id = Objects.requireNonNull(id, "UserSession id cannot be null");
        this.accountId = Objects.requireNonNull(accountId, "UserSession accountId cannot be null");
        this.deviceId = Objects.requireNonNull(deviceId, "UserSession deviceId cannot be null");
        this.deviceName = deviceName;
        this.refreshTokenHash = Objects.requireNonNull(refreshTokenHash, "refreshTokenHash cannot be null");
        this.previousRefreshTokenHash = previousRefreshTokenHash;
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt cannot be null");
        this.isRevoked = isRevoked;
        this.revokedAt = revokedAt;
        this.revocationReason = revocationReason;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.lastRefreshedAt = lastRefreshedAt != null ? lastRefreshedAt : this.createdAt;
    }

    public static UserSession createNew(
            UUID id,
            UUID accountId,
            String deviceId,
            String deviceName,
            String refreshTokenHash,
            Duration sessionLifespan
    ) {
        Instant now = Instant.now();
        return new UserSession(
                id,
                accountId,
                deviceId,
                deviceName,
                refreshTokenHash,
                null,
                now.plus(sessionLifespan),
                false,
                null,
                null,
                now,
                now
        );
    }

    public boolean isExpired() {
        return expiresAt.isBefore(Instant.now());
    }

    public boolean isValid() {
        return !isRevoked && !isExpired();
    }

    public boolean isWithinGraceWindow(int graceWindowSeconds) {
        Instant now = Instant.now();
        return !now.isAfter(lastRefreshedAt.plusSeconds(graceWindowSeconds));
    }

    public void rotate(String newRefreshTokenHash, Duration sessionLifespan) {
        this.previousRefreshTokenHash = this.refreshTokenHash;
        this.refreshTokenHash = Objects.requireNonNull(newRefreshTokenHash, "newRefreshTokenHash cannot be null");
        this.lastRefreshedAt = Instant.now();
        this.expiresAt = this.lastRefreshedAt.plus(sessionLifespan);
    }

    public void revoke(String reason) {
        this.isRevoked = true;
        this.revokedAt = Instant.now();
        this.revocationReason = reason;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getRefreshTokenHash() {
        return refreshTokenHash;
    }

    public void setRefreshTokenHash(String refreshTokenHash) {
        this.refreshTokenHash = refreshTokenHash;
    }

    public String getPreviousRefreshTokenHash() {
        return previousRefreshTokenHash;
    }

    public void setPreviousRefreshTokenHash(String previousRefreshTokenHash) {
        this.previousRefreshTokenHash = previousRefreshTokenHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isRevoked() {
        return isRevoked;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public String getRevocationReason() {
        return revocationReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastRefreshedAt() {
        return lastRefreshedAt;
    }

    public void setLastRefreshedAt(Instant lastRefreshedAt) {
        this.lastRefreshedAt = lastRefreshedAt;
    }
}
