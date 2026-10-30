package dev.ordago.auth.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import dev.ordago.auth.domain.exception.InvalidEmailException;
import dev.ordago.auth.domain.exception.InvalidPasswordHashException;
import dev.ordago.auth.domain.exception.InvalidPasswordResetTokenException;
import dev.ordago.auth.domain.exception.InvalidVerificationTokenException;
import dev.ordago.auth.domain.exception.PasswordResetTokenExpiredException;
import dev.ordago.auth.domain.exception.VerificationTokenExpiredException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountTest {

    @Test
    void shouldCreateNewAccountWithNormalizedEmail() {
        UUID id = UUID.randomUUID();
        Account account = Account.createNew(id, "  Player@Domain.DEV  ", "hash123");

        assertThat(account.getId()).isEqualTo(id);
        assertThat(account.getEmail().value()).isEqualTo("player@domain.dev");
        assertThat(account.getPasswordHash().value()).isEqualTo("hash123");
        assertThat(account.isEmailVerified()).isFalse();
        assertThat(account.isLocked()).isFalse();
        assertThat(account.isBanned()).isFalse();
        assertThat(account.isDeleted()).isFalse();
        assertThat(account.getFailedLoginAttempts()).isEqualTo(0);
        assertThat(account.getEmailVerificationToken()).isNotNull();
    }

    @Test
    void shouldTrackFailedLoginAttemptsAndLockAccountAfterThreshold() {
        Account account = Account.createNew(UUID.randomUUID(), "lockout@ordago.dev", "hash");
        Duration lockDuration = Duration.ofMinutes(15);

        for (int i = 1; i <= 4; i++) {
            account.recordFailedLogin(5, lockDuration);
            assertThat(account.getFailedLoginAttempts()).isEqualTo(i);
            assertThat(account.isLocked()).isFalse();
        }

        // 5th attempt locks the account
        account.recordFailedLogin(5, lockDuration);
        assertThat(account.getFailedLoginAttempts()).isEqualTo(5);
        assertThat(account.isLocked()).isTrue();
        assertThat(account.getLockedUntil()).isAfter(Instant.now());
    }

    @Test
    void shouldResetFailedAttemptsOnSuccessfulLogin() {
        Account account = Account.createNew(UUID.randomUUID(), "success@ordago.dev", "hash");
        account.recordFailedLogin(5, Duration.ofMinutes(15));
        account.recordFailedLogin(5, Duration.ofMinutes(15));
        assertThat(account.getFailedLoginAttempts()).isEqualTo(2);

        account.recordSuccessfulLogin("192.168.1.100");

        assertThat(account.getFailedLoginAttempts()).isEqualTo(0);
        assertThat(account.getLockedUntil()).isNull();
        assertThat(account.getLastLoginIp()).isEqualTo("192.168.1.100");
        assertThat(account.getLastLoginAt()).isNotNull();
    }

    @Test
    void shouldUnlockAccount() {
        Account account = Account.createNew(UUID.randomUUID(), "unlock@ordago.dev", "hash");
        account.recordFailedLogin(1, Duration.ofMinutes(15));
        assertThat(account.isLocked()).isTrue();

        account.unlock();

        assertThat(account.isLocked()).isFalse();
        assertThat(account.getFailedLoginAttempts()).isEqualTo(0);
        assertThat(account.getLockedUntil()).isNull();
    }

    @Test
    void shouldCheckIfBanned() {
        Account account = Account.createNew(UUID.randomUUID(), "ban@ordago.dev", "hash");
        assertThat(account.isBanned()).isFalse();

        // Account banned indefinitely
        Account bannedForever = new Account(
                account.getId(),
                account.getEmail(),
                account.getPasswordHash(),
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
                true,
                null,
                "Cheating",
                account.getCreatedAt(),
                account.getUpdatedAt(),
                null
        );
        assertThat(bannedForever.isBanned()).isTrue();

        // Account banned with future expiration
        Account bannedTemporary = new Account(
                account.getId(),
                account.getEmail(),
                account.getPasswordHash(),
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
                true,
                Instant.now().plusSeconds(3600),
                "Cheating",
                account.getCreatedAt(),
                account.getUpdatedAt(),
                null
        );
        assertThat(bannedTemporary.isBanned()).isTrue();

        // Account banned but expiration passed
        Account banExpired = new Account(
                account.getId(),
                account.getEmail(),
                account.getPasswordHash(),
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
                true,
                Instant.now().minusSeconds(10),
                "Cheating",
                account.getCreatedAt(),
                account.getUpdatedAt(),
                null
        );
        assertThat(banExpired.isBanned()).isFalse();
    }

    @Test
    void shouldVerifyEmail() {
        Account account = Account.createNew(UUID.randomUUID(), "verify@ordago.dev", "hash");
        assertThat(account.isEmailVerified()).isFalse();
        assertThat(account.getEmailVerificationToken()).isNotNull();

        account.verifyEmail();

        assertThat(account.isEmailVerified()).isTrue();
        assertThat(account.getEmailVerificationToken()).isNull();
        assertThat(account.getEmailVerificationExpiresAt()).isNull();
    }

    @Test
    void shouldSoftDeleteAccount() {
        Account account = Account.createNew(UUID.randomUUID(), "delete@ordago.dev", "hash");
        assertThat(account.isDeleted()).isFalse();

        account.softDelete();

        assertThat(account.isDeleted()).isTrue();
    }

    @Test
    void shouldRejectNullMandatoryFields() {
        UUID id = UUID.randomUUID();
        Email email = Email.of("email@ordago.dev");
        PasswordHash hash = PasswordHash.of("hash");

        assertThatThrownBy(() -> Account.createNew(null, email, hash))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Account.createNew(id, (Email) null, hash))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Account.createNew(id, email, (PasswordHash) null))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> Account.createNew(id, (String) null, "hash"))
                .isInstanceOf(InvalidEmailException.class);
        assertThatThrownBy(() -> Account.createNew(id, "email@ordago.dev", (String) null))
                .isInstanceOf(InvalidPasswordHashException.class);
    }

    @Test
    void shouldGenerateEmailVerificationToken() {
        Account account = Account.createNew(UUID.randomUUID(), "token_gen@ordago.dev", "hash");
        account.generateEmailVerificationToken(Duration.ofHours(12));

        assertThat(account.getEmailVerificationToken()).isNotBlank();
        assertThat(account.getEmailVerificationExpiresAt()).isAfter(Instant.now());
    }

    @Test
    void shouldVerifyEmailWithValidToken() {
        Account account = Account.createNew(UUID.randomUUID(), "verify_token@ordago.dev", "hash");
        account.generateEmailVerificationToken(Duration.ofHours(24));
        String token = account.getEmailVerificationToken();

        account.verifyEmail(token);

        assertThat(account.isEmailVerified()).isTrue();
        assertThat(account.getEmailVerificationToken()).isNull();
        assertThat(account.getEmailVerificationExpiresAt()).isNull();
    }

    @Test
    void shouldRejectVerifyEmailWithInvalidToken() {
        Account account = Account.createNew(UUID.randomUUID(), "verify_bad@ordago.dev", "hash");
        account.generateEmailVerificationToken(Duration.ofHours(24));

        assertThatThrownBy(() -> account.verifyEmail("wrong-token"))
                .isInstanceOf(InvalidVerificationTokenException.class);
        assertThatThrownBy(() -> account.verifyEmail(null))
                .isInstanceOf(InvalidVerificationTokenException.class);
        assertThatThrownBy(() -> account.verifyEmail("   "))
                .isInstanceOf(InvalidVerificationTokenException.class);
    }

    @Test
    void shouldRejectVerifyEmailWithExpiredToken() {
        UUID id = UUID.randomUUID();
        Account account = new Account(
                id,
                Email.of("expired_verify@ordago.dev"),
                PasswordHash.of("hash"),
                false,
                false,
                "expired-token",
                Instant.now().minusSeconds(120),
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
                Instant.now(),
                Instant.now(),
                null
        );

        assertThatThrownBy(() -> account.verifyEmail("expired-token"))
                .isInstanceOf(VerificationTokenExpiredException.class);
    }

    @Test
    void shouldGeneratePasswordResetToken() {
        Account account = Account.createNew(UUID.randomUUID(), "reset_gen@ordago.dev", "hash");
        account.generatePasswordResetToken(Duration.ofMinutes(45));

        assertThat(account.getPasswordResetToken()).isNotBlank();
        assertThat(account.getPasswordResetExpiresAt()).isAfter(Instant.now());
    }

    @Test
    void shouldResetPasswordWithValidToken() {
        Account account = Account.createNew(UUID.randomUUID(), "reset_ok@ordago.dev", "old-hash");
        account.generatePasswordResetToken(Duration.ofMinutes(60));
        String token = account.getPasswordResetToken();

        PasswordHash newHash = PasswordHash.of("new-hashed-password");
        account.resetPassword(token, newHash);

        assertThat(account.getPasswordHash().value()).isEqualTo("new-hashed-password");
        assertThat(account.getPasswordResetToken()).isNull();
        assertThat(account.getPasswordResetExpiresAt()).isNull();
    }

    @Test
    void shouldRejectResetPasswordWithInvalidToken() {
        Account account = Account.createNew(UUID.randomUUID(), "reset_bad@ordago.dev", "old-hash");
        account.generatePasswordResetToken(Duration.ofMinutes(60));
        PasswordHash newHash = PasswordHash.of("new-hashed-password");

        assertThatThrownBy(() -> account.resetPassword("wrong-reset-token", newHash))
                .isInstanceOf(InvalidPasswordResetTokenException.class);
        assertThatThrownBy(() -> account.resetPassword(null, newHash))
                .isInstanceOf(InvalidPasswordResetTokenException.class);
        assertThatThrownBy(() -> account.resetPassword("", newHash))
                .isInstanceOf(InvalidPasswordResetTokenException.class);
    }

    @Test
    void shouldRejectResetPasswordWithExpiredToken() {
        UUID id = UUID.randomUUID();
        Account account = new Account(
                id,
                Email.of("expired_reset@ordago.dev"),
                PasswordHash.of("hash"),
                false,
                false,
                null,
                null,
                "expired-reset-token",
                Instant.now().minusSeconds(30),
                0,
                null,
                null,
                null,
                false,
                null,
                false,
                null,
                null,
                Instant.now(),
                Instant.now(),
                null
        );

        PasswordHash newHash = PasswordHash.of("new-hashed-password");
        assertThatThrownBy(() -> account.resetPassword("expired-reset-token", newHash))
                .isInstanceOf(PasswordResetTokenExpiredException.class);
    }

    @Test
    void shouldChangePassword() {
        Account account = Account.createNew(UUID.randomUUID(), "change_pw@ordago.dev", "old-hash");
        PasswordHash newHash = PasswordHash.of("changed-hash-123");

        account.changePassword(newHash);

        assertThat(account.getPasswordHash().value()).isEqualTo("changed-hash-123");
        assertThatThrownBy(() -> account.changePassword(null))
                .isInstanceOf(NullPointerException.class);
    }
}
