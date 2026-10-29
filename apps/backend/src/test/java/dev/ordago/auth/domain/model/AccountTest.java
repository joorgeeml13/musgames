package dev.ordago.auth.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import dev.ordago.auth.domain.exception.InvalidEmailException;
import dev.ordago.auth.domain.exception.InvalidPasswordHashException;
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
}
