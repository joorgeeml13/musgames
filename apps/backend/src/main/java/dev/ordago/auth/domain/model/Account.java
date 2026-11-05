package dev.ordago.auth.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import dev.ordago.auth.domain.exception.AccountNotGuestException;
import dev.ordago.auth.domain.exception.InvalidPasswordResetTokenException;
import dev.ordago.auth.domain.exception.InvalidVerificationTokenException;
import dev.ordago.auth.domain.exception.PasswordResetTokenExpiredException;
import dev.ordago.auth.domain.exception.VerificationTokenExpiredException;

public class Account {

    private final UUID id;
    private Email email;
    private PasswordHash passwordHash;
    private boolean isGuest;
    private boolean isSystemAdmin;
    private boolean emailVerified;
    private String emailVerificationToken;
    private Instant emailVerificationExpiresAt;
    private String passwordResetToken;
    private Instant passwordResetExpiresAt;
    private int failedLoginAttempts;
    private Instant lockedUntil;
    private Instant lastLoginAt;
    private String lastLoginIp;
    private boolean twoFactorEnabled;
    private String twoFactorSecret;
    private boolean isBanned;
    private Instant bannedUntil;
    private String banReason;
    private final Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;

    public Account(
            UUID id,
            Email email,
            PasswordHash passwordHash,
            boolean isGuest,
            boolean isSystemAdmin,
            boolean emailVerified,
            String emailVerificationToken,
            Instant emailVerificationExpiresAt,
            String passwordResetToken,
            Instant passwordResetExpiresAt,
            int failedLoginAttempts,
            Instant lockedUntil,
            Instant lastLoginAt,
            String lastLoginIp,
            boolean twoFactorEnabled,
            String twoFactorSecret,
            boolean isBanned,
            Instant bannedUntil,
            String banReason,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt
    ) {
        this.id = Objects.requireNonNull(id, "Account id cannot be null");
        this.isGuest = isGuest;
        if (!isGuest) {
            this.email = Objects.requireNonNull(email, "Account email cannot be null for regular account");
        } else {
            this.email = email;
        }
        this.passwordHash = passwordHash;
        this.isSystemAdmin = isSystemAdmin;
        this.emailVerified = emailVerified;
        this.emailVerificationToken = emailVerificationToken;
        this.emailVerificationExpiresAt = emailVerificationExpiresAt;
        this.passwordResetToken = passwordResetToken;
        this.passwordResetExpiresAt = passwordResetExpiresAt;
        this.failedLoginAttempts = failedLoginAttempts;
        this.lockedUntil = lockedUntil;
        this.lastLoginAt = lastLoginAt;
        this.lastLoginIp = lastLoginIp;
        this.twoFactorEnabled = twoFactorEnabled;
        this.twoFactorSecret = twoFactorSecret;
        this.isBanned = isBanned;
        this.bannedUntil = bannedUntil;
        this.banReason = banReason;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
        this.deletedAt = deletedAt;
    }

    public Account(
            UUID id,
            Email email,
            PasswordHash passwordHash,
            boolean isSystemAdmin,
            boolean emailVerified,
            String emailVerificationToken,
            Instant emailVerificationExpiresAt,
            String passwordResetToken,
            Instant passwordResetExpiresAt,
            int failedLoginAttempts,
            Instant lockedUntil,
            Instant lastLoginAt,
            String lastLoginIp,
            boolean twoFactorEnabled,
            String twoFactorSecret,
            boolean isBanned,
            Instant bannedUntil,
            String banReason,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt
    ) {
        this(
                id,
                email,
                passwordHash,
                false,
                isSystemAdmin,
                emailVerified,
                emailVerificationToken,
                emailVerificationExpiresAt,
                passwordResetToken,
                passwordResetExpiresAt,
                failedLoginAttempts,
                lockedUntil,
                lastLoginAt,
                lastLoginIp,
                twoFactorEnabled,
                twoFactorSecret,
                isBanned,
                bannedUntil,
                banReason,
                createdAt,
                updatedAt,
                deletedAt
        );
    }

    public Account(
            UUID id,
            String email,
            String passwordHash,
            boolean isGuest,
            boolean isSystemAdmin,
            boolean emailVerified,
            String emailVerificationToken,
            Instant emailVerificationExpiresAt,
            String passwordResetToken,
            Instant passwordResetExpiresAt,
            int failedLoginAttempts,
            Instant lockedUntil,
            Instant lastLoginAt,
            String lastLoginIp,
            boolean twoFactorEnabled,
            String twoFactorSecret,
            boolean isBanned,
            Instant bannedUntil,
            String banReason,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt
    ) {
        this(
                id,
                email != null ? Email.of(email) : null,
                passwordHash != null ? PasswordHash.of(passwordHash) : null,
                isGuest,
                isSystemAdmin,
                emailVerified,
                emailVerificationToken,
                emailVerificationExpiresAt,
                passwordResetToken,
                passwordResetExpiresAt,
                failedLoginAttempts,
                lockedUntil,
                lastLoginAt,
                lastLoginIp,
                twoFactorEnabled,
                twoFactorSecret,
                isBanned,
                bannedUntil,
                banReason,
                createdAt,
                updatedAt,
                deletedAt
        );
    }

    public Account(
            UUID id,
            String email,
            String passwordHash,
            boolean isSystemAdmin,
            boolean emailVerified,
            String emailVerificationToken,
            Instant emailVerificationExpiresAt,
            String passwordResetToken,
            Instant passwordResetExpiresAt,
            int failedLoginAttempts,
            Instant lockedUntil,
            Instant lastLoginAt,
            String lastLoginIp,
            boolean twoFactorEnabled,
            String twoFactorSecret,
            boolean isBanned,
            Instant bannedUntil,
            String banReason,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt
    ) {
        this(
                id,
                email,
                passwordHash,
                false,
                isSystemAdmin,
                emailVerified,
                emailVerificationToken,
                emailVerificationExpiresAt,
                passwordResetToken,
                passwordResetExpiresAt,
                failedLoginAttempts,
                lockedUntil,
                lastLoginAt,
                lastLoginIp,
                twoFactorEnabled,
                twoFactorSecret,
                isBanned,
                bannedUntil,
                banReason,
                createdAt,
                updatedAt,
                deletedAt
        );
    }

    public static Account createNew(UUID id, Email email, PasswordHash passwordHash) {
        Objects.requireNonNull(id, "Account id cannot be null");
        Objects.requireNonNull(email, "Account email cannot be null for regular account");
        Objects.requireNonNull(passwordHash, "Account passwordHash cannot be null for regular account");
        Instant now = Instant.now();
        return new Account(
                id,
                email,
                passwordHash,
                false,
                false,
                false,
                UUID.randomUUID().toString(),
                now.plus(Duration.ofHours(24)),
                null,
                null,
                0,
                null,
                null,
                null,
                false,
                null,
                false,
                null,
                null,
                now,
                now,
                null
        );
    }

    public static Account createNew(UUID id, String email, String passwordHash) {
        return createNew(id, Email.of(email), PasswordHash.of(passwordHash));
    }

    public static Account createGuest(UUID id, Instant now) {
        Instant createdAt = now != null ? now : Instant.now();
        return new Account(
                id,
                (Email) null,
                (PasswordHash) null,
                true,
                false,
                false,
                null,
                null,
                null,
                null,
                0,
                null,
                null,
                null,
                false,
                null,
                false,
                null,
                null,
                createdAt,
                createdAt,
                null
        );
    }

    public static Account createGuest(UUID id) {
        return createGuest(id, Instant.now());
    }

    public static Account createSocial(UUID id, Email email, Instant now) {
        Objects.requireNonNull(id, "Account id cannot be null");
        Objects.requireNonNull(email, "Account email cannot be null");
        Instant createdAt = now != null ? now : Instant.now();
        return new Account(
                id,
                email,
                (PasswordHash) null,
                false,
                false,
                true,
                null,
                null,
                null,
                null,
                0,
                null,
                null,
                null,
                false,
                null,
                false,
                null,
                null,
                createdAt,
                createdAt,
                null
        );
    }

    public static Account createSocial(UUID id, String email) {
        return createSocial(id, Email.of(email), Instant.now());
    }

    public void claimAsRegular(Email newEmail, PasswordHash newPasswordHash, Duration verificationTokenDuration) {
        if (!this.isGuest) {
            throw new AccountNotGuestException();
        }
        this.email = Objects.requireNonNull(newEmail, "newEmail cannot be null");
        this.passwordHash = Objects.requireNonNull(newPasswordHash, "newPasswordHash cannot be null");
        this.isGuest = false;
        generateEmailVerificationToken(verificationTokenDuration);
        this.updatedAt = Instant.now();
    }

    public void claimAsRegular(String newEmail, String newPasswordHash, Duration verificationTokenDuration) {
        claimAsRegular(Email.of(newEmail), PasswordHash.of(newPasswordHash), verificationTokenDuration);
    }

    public void claimAsSocial(Email email) {
        if (!this.isGuest) {
            throw new AccountNotGuestException();
        }
        this.email = Objects.requireNonNull(email, "email cannot be null");
        this.passwordHash = null;
        this.isGuest = false;
        this.emailVerified = true;
        this.emailVerificationToken = null;
        this.emailVerificationExpiresAt = null;
        this.updatedAt = Instant.now();
    }

    public void claimAsSocial(String email) {
        claimAsSocial(Email.of(email));
    }

    public boolean isGuest() {
        return isGuest;
    }

    public boolean isLocked() {
        return lockedUntil != null && lockedUntil.isAfter(Instant.now());
    }

    public boolean isBanned() {
        return isBanned && (bannedUntil == null || bannedUntil.isAfter(Instant.now()));
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void recordSuccessfulLogin(String clientIp) {
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
        this.lastLoginAt = Instant.now();
        this.lastLoginIp = clientIp;
        this.updatedAt = this.lastLoginAt;
    }

    public void recordFailedLogin(int maxAttempts, Duration lockDuration) {
        this.failedLoginAttempts++;
        if (this.failedLoginAttempts >= maxAttempts) {
            this.lockedUntil = Instant.now().plus(lockDuration);
        }
        this.updatedAt = Instant.now();
    }

    public void unlock() {
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
        this.updatedAt = Instant.now();
    }

    public void softDelete() {
        this.deletedAt = Instant.now();
        this.updatedAt = this.deletedAt;
    }

    public void generateEmailVerificationToken(Duration validity) {
        Objects.requireNonNull(validity, "validity cannot be null");
        this.emailVerificationToken = UUID.randomUUID().toString();
        this.emailVerificationExpiresAt = Instant.now().plus(validity);
        this.updatedAt = Instant.now();
    }

    public void verifyEmail() {
        this.emailVerified = true;
        this.emailVerificationToken = null;
        this.emailVerificationExpiresAt = null;
        this.updatedAt = Instant.now();
    }

    public void markEmailVerified() {
        verifyEmail();
    }

    public void verifyEmail(String token) {
        if (token == null || token.isBlank() || this.emailVerificationToken == null || !this.emailVerificationToken.equals(token)) {
            throw new InvalidVerificationTokenException();
        }
        if (this.emailVerificationExpiresAt == null || !this.emailVerificationExpiresAt.isAfter(Instant.now())) {
            throw new VerificationTokenExpiredException();
        }
        this.emailVerified = true;
        this.emailVerificationToken = null;
        this.emailVerificationExpiresAt = null;
        this.updatedAt = Instant.now();
    }

    public void generatePasswordResetToken(Duration validity) {
        Objects.requireNonNull(validity, "validity cannot be null");
        this.passwordResetToken = UUID.randomUUID().toString();
        this.passwordResetExpiresAt = Instant.now().plus(validity);
        this.updatedAt = Instant.now();
    }

    public void resetPassword(String token, PasswordHash newPasswordHash) {
        Objects.requireNonNull(newPasswordHash, "newPasswordHash cannot be null");
        if (token == null || token.isBlank() || this.passwordResetToken == null || !this.passwordResetToken.equals(token)) {
            throw new InvalidPasswordResetTokenException();
        }
        if (this.passwordResetExpiresAt == null || !this.passwordResetExpiresAt.isAfter(Instant.now())) {
            throw new PasswordResetTokenExpiredException();
        }
        this.passwordHash = newPasswordHash;
        this.passwordResetToken = null;
        this.passwordResetExpiresAt = null;
        this.updatedAt = Instant.now();
    }

    public void changePassword(PasswordHash newPasswordHash) {
        this.passwordHash = Objects.requireNonNull(newPasswordHash, "newPasswordHash cannot be null");
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Email getEmail() {
        if (isGuest && email == null) {
            return Email.empty();
        }
        return email;
    }

    public String getEmailValue() {
        return email != null ? email.value() : null;
    }

    public PasswordHash getPasswordHash() {
        return passwordHash;
    }

    public String getPasswordHashValue() {
        return passwordHash != null ? passwordHash.value() : null;
    }

    public void setPasswordHash(PasswordHash passwordHash) {
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash cannot be null");
        this.updatedAt = Instant.now();
    }

    public void setPasswordHash(String passwordHash) {
        setPasswordHash(PasswordHash.of(passwordHash));
    }

    public boolean isSystemAdmin() {
        return isSystemAdmin;
    }

    public void setSystemAdmin(boolean systemAdmin) {
        isSystemAdmin = systemAdmin;
        this.updatedAt = Instant.now();
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public String getEmailVerificationToken() {
        return emailVerificationToken;
    }

    public Instant getEmailVerificationExpiresAt() {
        return emailVerificationExpiresAt;
    }

    public String getPasswordResetToken() {
        return passwordResetToken;
    }

    public Instant getPasswordResetExpiresAt() {
        return passwordResetExpiresAt;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public String getLastLoginIp() {
        return lastLoginIp;
    }

    public boolean isTwoFactorEnabled() {
        return twoFactorEnabled;
    }

    public String getTwoFactorSecret() {
        return twoFactorSecret;
    }

    public Instant getBannedUntil() {
        return bannedUntil;
    }

    public String getBanReason() {
        return banReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }
}
