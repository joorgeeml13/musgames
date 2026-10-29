package dev.ordago.auth.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import dev.ordago.auth.domain.model.Account;

@Entity
@Table(
        name = "accounts",
        indexes = {
                @Index(name = "idx_accounts_email", columnList = "email", unique = true)
        }
)
public class AccountEntity extends PanacheEntityBase {

    @Id
    @Column(name = "id", nullable = false)
    public UUID id;

    @Version
    @Column(name = "version")
    public Long version;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    public String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    public String passwordHash;

    @Column(name = "is_system_admin", nullable = false)
    public boolean isSystemAdmin = false;

    @Column(name = "email_verified", nullable = false)
    public boolean emailVerified = false;

    @Column(name = "email_verification_token", length = 255)
    public String emailVerificationToken;

    @Column(name = "email_verification_expires_at")
    public Instant emailVerificationExpiresAt;

    @Column(name = "password_reset_token", length = 255)
    public String passwordResetToken;

    @Column(name = "password_reset_expires_at")
    public Instant passwordResetExpiresAt;

    @Column(name = "failed_login_attempts", nullable = false)
    public int failedLoginAttempts = 0;

    @Column(name = "locked_until")
    public Instant lockedUntil;

    @Column(name = "last_login_at")
    public Instant lastLoginAt;

    @Column(name = "last_login_ip", length = 45)
    public String lastLoginIp;

    @Column(name = "two_factor_enabled", nullable = false)
    public boolean twoFactorEnabled = false;

    @Column(name = "two_factor_secret", length = 255)
    public String twoFactorSecret;

    @Column(name = "is_banned", nullable = false)
    public boolean isBanned = false;

    @Column(name = "banned_until")
    public Instant bannedUntil;

    @Column(name = "ban_reason", length = 255)
    public String banReason;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;

    @Column(name = "deleted_at")
    public Instant deletedAt;

    public Account toDomain() {
        return new Account(
                this.id,
                this.email,
                this.passwordHash,
                this.isSystemAdmin,
                this.emailVerified,
                this.emailVerificationToken,
                this.emailVerificationExpiresAt,
                this.passwordResetToken,
                this.passwordResetExpiresAt,
                this.failedLoginAttempts,
                this.lockedUntil,
                this.lastLoginAt,
                this.lastLoginIp,
                this.twoFactorEnabled,
                this.twoFactorSecret,
                this.isBanned,
                this.bannedUntil,
                this.banReason,
                this.createdAt,
                this.updatedAt,
                this.deletedAt
        );
    }

    public static AccountEntity fromDomain(Account domain) {
        AccountEntity entity = new AccountEntity();
        entity.id = domain.getId();
        entity.email = domain.getEmail().value();
        entity.passwordHash = domain.getPasswordHash().value();
        entity.isSystemAdmin = domain.isSystemAdmin();
        entity.emailVerified = domain.isEmailVerified();
        entity.emailVerificationToken = domain.getEmailVerificationToken();
        entity.emailVerificationExpiresAt = domain.getEmailVerificationExpiresAt();
        entity.passwordResetToken = domain.getPasswordResetToken();
        entity.passwordResetExpiresAt = domain.getPasswordResetExpiresAt();
        entity.failedLoginAttempts = domain.getFailedLoginAttempts();
        entity.lockedUntil = domain.getLockedUntil();
        entity.lastLoginAt = domain.getLastLoginAt();
        entity.lastLoginIp = domain.getLastLoginIp();
        entity.twoFactorEnabled = domain.isTwoFactorEnabled();
        entity.twoFactorSecret = domain.getTwoFactorSecret();
        entity.isBanned = domain.isBanned();
        entity.bannedUntil = domain.getBannedUntil();
        entity.banReason = domain.getBanReason();
        entity.createdAt = domain.getCreatedAt();
        entity.updatedAt = domain.getUpdatedAt();
        entity.deletedAt = domain.getDeletedAt();
        return entity;
    }
}
