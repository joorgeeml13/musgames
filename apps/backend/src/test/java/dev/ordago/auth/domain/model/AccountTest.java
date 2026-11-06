package dev.ordago.auth.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import dev.ordago.auth.domain.exception.AccountNotGuestException;
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
        assertThat(account.getEmailVerificationTokenHash()).isNull();
        assertThat(account.getEmailVerificationExpiresAt()).isNull();
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
        account.generateEmailVerificationToken(Duration.ofHours(24));
        assertThat(account.isEmailVerified()).isFalse();
        assertThat(account.getEmailVerificationTokenHash()).isNotNull();

        account.verifyEmail();

        assertThat(account.isEmailVerified()).isTrue();
        assertThat(account.getEmailVerificationTokenHash()).isNull();
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
        String plainToken = account.generateEmailVerificationToken(Duration.ofHours(12));

        assertThat(plainToken).isNotBlank();
        assertThat(account.getEmailVerificationTokenHash()).isNotBlank();
        assertThat(account.getEmailVerificationTokenHash()).hasSize(64);
        assertThat(account.getEmailVerificationExpiresAt()).isAfter(Instant.now());
    }

    @Test
    void shouldVerifyEmailWithValidToken() {
        Account account = Account.createNew(UUID.randomUUID(), "verify_token@ordago.dev", "hash");
        String token = account.generateEmailVerificationToken(Duration.ofHours(24));

        account.verifyEmail(token);

        assertThat(account.isEmailVerified()).isTrue();
        assertThat(account.getEmailVerificationTokenHash()).isNull();
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
        Account account = Account.createNew(id, "expired_verify@ordago.dev", "hash");
        String token = account.generateEmailVerificationToken(Duration.ofSeconds(-10));

        assertThatThrownBy(() -> account.verifyEmail(token))
                .isInstanceOf(VerificationTokenExpiredException.class);
    }

    @Test
    void shouldGeneratePasswordResetToken() {
        Account account = Account.createNew(UUID.randomUUID(), "reset_gen@ordago.dev", "hash");
        String plainToken = account.generatePasswordResetToken(Duration.ofMinutes(45));

        assertThat(plainToken).isNotBlank();
        assertThat(account.getPasswordResetTokenHash()).isNotBlank();
        assertThat(account.getPasswordResetTokenHash()).hasSize(64);
        assertThat(account.getPasswordResetExpiresAt()).isAfter(Instant.now());
    }

    @Test
    void shouldResetPasswordWithValidToken() {
        Account account = Account.createNew(UUID.randomUUID(), "reset_ok@ordago.dev", "old-hash");
        String token = account.generatePasswordResetToken(Duration.ofMinutes(60));

        PasswordHash newHash = PasswordHash.of("new-hashed-password");
        account.resetPassword(token, newHash);

        assertThat(account.getPasswordHash().value()).isEqualTo("new-hashed-password");
        assertThat(account.getPasswordResetTokenHash()).isNull();
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
        Account account = Account.createNew(id, "expired_reset@ordago.dev", "hash");
        String token = account.generatePasswordResetToken(Duration.ofSeconds(-10));

        PasswordHash newHash = PasswordHash.of("new-hashed-password");
        assertThatThrownBy(() -> account.resetPassword(token, newHash))
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

    @Test
    void shouldCreateGuestAccountWithNullEmailAndPassword() {
        // Arrange
        UUID id = UUID.randomUUID();
        Instant fixedInstant = Instant.parse("2026-11-01T10:00:00Z");

        // Act
        Account guestAccount = Account.createGuest(id, fixedInstant);

        // Assert
        assertThat(guestAccount.getId()).isEqualTo(id);
        assertThat(guestAccount.isGuest()).isTrue();
        assertThat(guestAccount.getEmail().value()).isNull();
        assertThat(guestAccount.getEmailValue()).isNull();
        assertThat(guestAccount.getPasswordHash()).isNull();
        assertThat(guestAccount.getPasswordHashValue()).isNull();
        assertThat(guestAccount.isEmailVerified()).isFalse();
        assertThat(guestAccount.isSystemAdmin()).isFalse();
        assertThat(guestAccount.isLocked()).isFalse();
        assertThat(guestAccount.isBanned()).isFalse();
        assertThat(guestAccount.isDeleted()).isFalse();
        assertThat(guestAccount.getFailedLoginAttempts()).isEqualTo(0);
        assertThat(guestAccount.getEmailVerificationTokenHash()).isNull();
        assertThat(guestAccount.getEmailVerificationExpiresAt()).isNull();
        assertThat(guestAccount.getCreatedAt()).isEqualTo(fixedInstant);
        assertThat(guestAccount.getUpdatedAt()).isEqualTo(fixedInstant);
    }

    @Test
    void shouldCreateGuestAccountWithDefaultTimestamp() {
        // Arrange
        UUID id = UUID.randomUUID();
        Instant before = Instant.now().minusSeconds(1);

        // Act
        Account guestAccount = Account.createGuest(id);

        // Assert
        assertThat(guestAccount.getId()).isEqualTo(id);
        assertThat(guestAccount.isGuest()).isTrue();
        assertThat(guestAccount.getEmail().value()).isNull();
        assertThat(guestAccount.getPasswordHash()).isNull();
        assertThat(guestAccount.getCreatedAt()).isAfterOrEqualTo(before);
    }

    @Test
    void shouldClaimGuestAccountSuccessfullyAsRegularAccount() {
        // Arrange
        UUID id = UUID.randomUUID();
        Account guestAccount = Account.createGuest(id);
        Email regularEmail = Email.of("guest_claimed@ordago.dev");
        PasswordHash regularPasswordHash = PasswordHash.of("argon2_claimed_hash");
        Duration tokenValidity = Duration.ofHours(24);

        // Act
        String plainToken = guestAccount.claimAsRegular(regularEmail, regularPasswordHash, tokenValidity);

        // Assert
        assertThat(guestAccount.isGuest())
                .as("Account must no longer be marked as guest")
                .isFalse();
        assertThat(guestAccount.getEmail().value()).isEqualTo("guest_claimed@ordago.dev");
        assertThat(guestAccount.getEmailValue()).isEqualTo("guest_claimed@ordago.dev");
        assertThat(guestAccount.getPasswordHash().value()).isEqualTo("argon2_claimed_hash");
        assertThat(guestAccount.getPasswordHashValue()).isEqualTo("argon2_claimed_hash");
        assertThat(guestAccount.isEmailVerified()).isFalse();
        assertThat(plainToken).isNotBlank();
        assertThat(guestAccount.getEmailVerificationTokenHash())
                .as("Verification token hash must be generated upon claiming regular account")
                .isNotBlank();
        assertThat(guestAccount.getEmailVerificationExpiresAt())
                .as("Verification token expiry must be set in the future")
                .isAfter(Instant.now());
    }

    @Test
    void shouldClaimGuestAccountUsingStringOverloadSuccessfully() {
        // Arrange
        UUID id = UUID.randomUUID();
        Account guestAccount = Account.createGuest(id);

        // Act
        String plainToken = guestAccount.claimAsRegular("string_claim@ordago.dev", "string_hash", Duration.ofHours(12));

        // Assert
        assertThat(guestAccount.isGuest()).isFalse();
        assertThat(guestAccount.getEmailValue()).isEqualTo("string_claim@ordago.dev");
        assertThat(guestAccount.getPasswordHashValue()).isEqualTo("string_hash");
        assertThat(plainToken).isNotBlank();
        assertThat(guestAccount.getEmailVerificationTokenHash()).isNotBlank();
        assertThat(guestAccount.getEmailVerificationExpiresAt()).isAfter(Instant.now());
    }

    @Test
    void shouldRejectClaimAsRegularWhenAccountIsNotGuest() {
        // Arrange: account created directly as regular
        UUID id = UUID.randomUUID();
        Account regularAccount = Account.createNew(id, "already_regular@ordago.dev", "regular_hash");
        Email newEmail = Email.of("new_email@ordago.dev");
        PasswordHash newHash = PasswordHash.of("new_hash");
        Duration validity = Duration.ofHours(24);

        // Act & Assert
        assertThatThrownBy(() -> regularAccount.claimAsRegular(newEmail, newHash, validity))
                .isInstanceOf(AccountNotGuestException.class);
    }

    @Test
    void shouldRejectSecondClaimWhenAccountAlreadyClaimed() {
        // Arrange: guest account claimed once
        UUID id = UUID.randomUUID();
        Account account = Account.createGuest(id);
        account.claimAsRegular("first_claim@ordago.dev", "hash_one", Duration.ofHours(24));
        assertThat(account.isGuest()).isFalse();

        // Act & Assert: attempting a second claim must fail
        assertThatThrownBy(() -> account.claimAsRegular("second_claim@ordago.dev", "hash_two", Duration.ofHours(24)))
                .isInstanceOf(AccountNotGuestException.class);
    }

    @Test
    void shouldRejectClaimWithNullArguments() {
        // Arrange
        Account guestAccount = Account.createGuest(UUID.randomUUID());
        Email validEmail = Email.of("valid@ordago.dev");
        PasswordHash validHash = PasswordHash.of("valid_hash");
        Duration validDuration = Duration.ofHours(24);

        // Act & Assert
        assertThatThrownBy(() -> guestAccount.claimAsRegular((Email) null, validHash, validDuration))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("newEmail cannot be null");

        assertThatThrownBy(() -> guestAccount.claimAsRegular(validEmail, (PasswordHash) null, validDuration))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("newPasswordHash cannot be null");

        assertThatThrownBy(() -> guestAccount.claimAsRegular(validEmail, validHash, (Duration) null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("validity cannot be null");
    }

    @Test
    void shouldCreateSocialAccountWithVerifiedEmailAndNoPasswordHash() {
        UUID id = UUID.randomUUID();
        Account socialAccount = Account.createSocial(id, "social@ordago.dev");

        assertThat(socialAccount.getId()).isEqualTo(id);
        assertThat(socialAccount.getEmailValue()).isEqualTo("social@ordago.dev");
        assertThat(socialAccount.getPasswordHash()).isNull();
        assertThat(socialAccount.isGuest()).isFalse();
        assertThat(socialAccount.isEmailVerified()).isTrue();
        assertThat(socialAccount.getEmailVerificationTokenHash()).isNull();
    }

    @Test
    void shouldClaimGuestAccountSuccessfullyAsSocialAccount() {
        UUID id = UUID.randomUUID();
        Account guestAccount = Account.createGuest(id);

        guestAccount.claimAsSocial("claimed_social@ordago.dev");

        assertThat(guestAccount.isGuest()).isFalse();
        assertThat(guestAccount.getEmailValue()).isEqualTo("claimed_social@ordago.dev");
        assertThat(guestAccount.getPasswordHash()).isNull();
        assertThat(guestAccount.isEmailVerified()).isTrue();
        assertThat(guestAccount.getEmailVerificationTokenHash()).isNull();
    }

    @Test
    void shouldRejectClaimAsSocialWhenAccountIsNotGuest() {
        UUID id = UUID.randomUUID();
        Account regular = Account.createNew(id, "regular@ordago.dev", "hash");

        assertThatThrownBy(() -> regular.claimAsSocial("social@ordago.dev"))
                .isInstanceOf(AccountNotGuestException.class);
    }
}
