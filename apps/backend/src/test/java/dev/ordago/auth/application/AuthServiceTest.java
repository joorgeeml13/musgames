package dev.ordago.auth.application;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import dev.ordago.auth.domain.exception.AccountBannedException;
import dev.ordago.auth.domain.exception.AccountLockedException;
import dev.ordago.auth.domain.exception.EmailAlreadyExistsException;
import dev.ordago.auth.domain.exception.InvalidCredentialsException;
import dev.ordago.auth.domain.exception.InvalidPasswordResetTokenException;
import dev.ordago.auth.domain.exception.InvalidVerificationTokenException;
import dev.ordago.auth.domain.exception.PasswordResetTokenExpiredException;
import dev.ordago.auth.domain.exception.SessionExpiredException;
import dev.ordago.auth.domain.exception.SessionNotFoundException;
import dev.ordago.auth.domain.exception.SessionRevokedException;
import dev.ordago.auth.domain.exception.VerificationTokenExpiredException;
import dev.ordago.auth.domain.model.Account;
import dev.ordago.auth.domain.model.AuthTokens;
import dev.ordago.auth.domain.model.Email;
import dev.ordago.auth.domain.model.PasswordHash;
import dev.ordago.auth.domain.model.UserSession;
import dev.ordago.auth.domain.port.in.LoginUseCase;
import dev.ordago.auth.domain.port.in.LogoutUseCase;
import dev.ordago.auth.domain.port.in.RefreshTokenUseCase;
import dev.ordago.auth.domain.port.in.RegisterUseCase;
import dev.ordago.auth.domain.port.in.RegisterUseCase.AuthResult;
import dev.ordago.auth.domain.port.out.AccountRepositoryPort;
import dev.ordago.auth.domain.port.out.PasswordEncoderPort;
import dev.ordago.auth.domain.port.out.TokenProviderPort;
import dev.ordago.auth.domain.port.out.UserSessionRepositoryPort;
import dev.ordago.notification.domain.port.in.SendEmailUseCase;
import dev.ordago.notification.domain.port.in.SendEmailUseCase.SendEmailCommand;
import dev.ordago.profile.domain.exception.PlayerTagAlreadyTakenException;
import dev.ordago.profile.domain.model.PlayerTag;
import dev.ordago.profile.domain.model.UserProfile;
import dev.ordago.profile.domain.port.in.CreateUserProfileUseCase;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthServiceTest {

    private AuthService authService;
    private InMemoryAccountRepository accountRepository;
    private InMemoryUserSessionRepository sessionRepository;
    private StubPasswordEncoder passwordEncoder;
    private StubTokenProvider tokenProvider;
    private StubCreateUserProfileUseCase createUserProfileUseCase;
    private StubSendEmailUseCase sendEmailUseCase;

    @BeforeEach
    void setUp() {
        accountRepository = new InMemoryAccountRepository();
        sessionRepository = new InMemoryUserSessionRepository();
        passwordEncoder = new StubPasswordEncoder();
        tokenProvider = new StubTokenProvider();
        createUserProfileUseCase = new StubCreateUserProfileUseCase();
        sendEmailUseCase = new StubSendEmailUseCase();

        authService = new AuthService(
                accountRepository,
                sessionRepository,
                passwordEncoder,
                tokenProvider,
                createUserProfileUseCase,
                sendEmailUseCase,
                5,
                15,
                30,
                24,
                60
        );
    }

    @Test
    void shouldRegisterNewAccountAndCreateDefaultProfileAndSession() {
        RegisterUseCase.RegisterCommand cmd = new RegisterUseCase.RegisterCommand(
                "player1@ordago.dev",
                "Password123!",
                "MusMaster",
                "ESP1",
                "device-1",
                "Chrome Desktop",
                "127.0.0.1"
        );

        RegisterUseCase.AuthResult result = authService.register(cmd);

        assertThat(result).isNotNull();
        assertThat(result.account().getEmail().value()).isEqualTo("player1@ordago.dev");
        assertThat(result.tokens().accessToken()).isEqualTo("access-token-" + result.account().getId());
        assertThat(result.tokens().refreshToken()).isNotNull();
        assertThat(createUserProfileUseCase.createdProfiles).containsKey(result.account().getId());
        assertThat(sessionRepository.sessions).containsKey(result.session().getId());
    }

    @Test
    void shouldRejectRegistrationWithDuplicateEmail() {
        RegisterUseCase.RegisterCommand cmd = new RegisterUseCase.RegisterCommand(
                "duplicate@ordago.dev",
                "Password123!",
                "MusMaster",
                "ESP1",
                "device-1",
                "Chrome Desktop",
                "127.0.0.1"
        );
        authService.register(cmd);

        assertThatThrownBy(() -> authService.register(cmd))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }

    @Test
    void shouldRejectRegistrationWithDuplicatePlayerTag() {
        authService.register(new RegisterUseCase.RegisterCommand(
                "player1@ordago.dev",
                "Password123!",
                "MusMaster",
                "ESP1",
                "device-1",
                "PC",
                "127.0.0.1"
        ));

        RegisterUseCase.RegisterCommand duplicatePlayerTagCmd = new RegisterUseCase.RegisterCommand(
                "player2@ordago.dev",
                "Password123!",
                "musmaster",
                "esp1",
                "device-2",
                "Phone",
                "127.0.0.1"
        );

        assertThatThrownBy(() -> authService.register(duplicatePlayerTagCmd))
                .isInstanceOf(PlayerTagAlreadyTakenException.class);
    }

    @Test
    void shouldVerifyPasswordHashingOnRegistration() {
        String plainPassword = "MySecretPassword123!";
        RegisterUseCase.RegisterCommand cmd = new RegisterUseCase.RegisterCommand(
                "hash_test@ordago.dev",
                plainPassword,
                "HashMaster",
                "01",
                "device-1",
                "PC",
                "127.0.0.1"
        );

        RegisterUseCase.AuthResult result = authService.register(cmd);

        assertThat(result.account().getPasswordHash().value()).isNotEqualTo(plainPassword);
        assertThat(passwordEncoder.matches(plainPassword, result.account().getPasswordHash().value())).isTrue();
    }

    @Test
    void shouldLoginSuccessfullyWithValidCredentials() {
        authService.register(new RegisterUseCase.RegisterCommand(
                "login@ordago.dev",
                "SecretPass!",
                "LoginUser",
                "01",
                "dev-1",
                "PC",
                "127.0.0.1"
        ));

        AuthResult loginResult = authService.login(new LoginUseCase.LoginCommand(
                "login@ordago.dev",
                "SecretPass!",
                "dev-2",
                "Phone",
                "192.168.1.1"
        ));

        assertThat(loginResult).isNotNull();
        assertThat(loginResult.tokens().accessToken()).isNotBlank();
        assertThat(loginResult.account().getLastLoginIp()).isEqualTo("192.168.1.1");
    }

    @Test
    void shouldRejectLoginWithInvalidPassword() {
        authService.register(new RegisterUseCase.RegisterCommand(
                "badpass@ordago.dev",
                "RightPassword!",
                "BadPassUser",
                "01",
                "dev-1",
                "PC",
                "127.0.0.1"
        ));

        assertThatThrownBy(() -> authService.login(new LoginUseCase.LoginCommand(
                "badpass@ordago.dev",
                "WrongPassword!",
                "dev-1",
                "PC",
                "127.0.0.1"
        ))).isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void shouldLockAccountAfterFiveFailedLoginAttempts() {
        authService.register(new RegisterUseCase.RegisterCommand(
                "lockout@ordago.dev",
                "CorrectPassword",
                "LockoutUser",
                "01",
                "dev-1",
                "PC",
                "127.0.0.1"
        ));

        for (int i = 1; i <= 4; i++) {
            assertThatThrownBy(() -> authService.login(new LoginUseCase.LoginCommand(
                    "lockout@ordago.dev",
                    "WrongPassword",
                    "dev-1",
                    "PC",
                    "127.0.0.1"
            ))).isInstanceOf(InvalidCredentialsException.class);
        }

        // 5th attempt locks the account
        assertThatThrownBy(() -> authService.login(new LoginUseCase.LoginCommand(
                    "lockout@ordago.dev",
                    "WrongPassword",
                    "dev-1",
                    "PC",
                    "127.0.0.1"
            ))).isInstanceOf(AccountLockedException.class);

        // Even with correct password, login is blocked
        assertThatThrownBy(() -> authService.login(new LoginUseCase.LoginCommand(
                    "lockout@ordago.dev",
                    "CorrectPassword",
                    "dev-1",
                    "PC",
                    "127.0.0.1"
            ))).isInstanceOf(AccountLockedException.class);
    }

    @Test
    void shouldUnlockAccountWhenLockDurationPasses() {
        authService.register(new RegisterUseCase.RegisterCommand(
                "unlock_test@ordago.dev",
                "RightPassword!",
                "UnlockUser",
                "01",
                "dev-1",
                "PC",
                "127.0.0.1"
        ));

        // Lock out account with 5 failed attempts
        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> authService.login(new LoginUseCase.LoginCommand(
                    "unlock_test@ordago.dev",
                    "WrongPassword!",
                    "dev-1",
                    "PC",
                    "127.0.0.1"
            ))).isInstanceOf(InvalidCredentialsException.class);
        }
        assertThatThrownBy(() -> authService.login(new LoginUseCase.LoginCommand(
                "unlock_test@ordago.dev",
                "WrongPassword!",
                "dev-1",
                "PC",
                "127.0.0.1"
        ))).isInstanceOf(AccountLockedException.class);

        // Fast forward time past lock duration
        Account account = accountRepository.findByEmail("unlock_test@ordago.dev").orElseThrow();
        Account expiredLockAccount = new Account(
                account.getId(),
                account.getEmail(),
                account.getPasswordHash(),
                account.isSystemAdmin(),
                account.isEmailVerified(),
                account.getEmailVerificationToken(),
                account.getEmailVerificationExpiresAt(),
                account.getPasswordResetToken(),
                account.getPasswordResetExpiresAt(),
                account.getFailedLoginAttempts(),
                Instant.now().minusSeconds(60),
                account.getLastLoginAt(),
                account.getLastLoginIp(),
                account.isTwoFactorEnabled(),
                account.getTwoFactorSecret(),
                account.isBanned(),
                account.getBannedUntil(),
                account.getBanReason(),
                account.getCreatedAt(),
                account.getUpdatedAt(),
                null
        );
        accountRepository.save(expiredLockAccount);

        // Login should now succeed and unlock
        AuthResult loginResult = authService.login(new LoginUseCase.LoginCommand(
                "unlock_test@ordago.dev",
                "RightPassword!",
                "dev-1",
                "PC",
                "127.0.0.1"
        ));

        assertThat(loginResult).isNotNull();
        assertThat(loginResult.account().isLocked()).isFalse();
        assertThat(loginResult.account().getFailedLoginAttempts()).isEqualTo(0);
    }

    @Test
    void shouldRejectLoginWhenAccountIsBanned() {
        authService.register(new RegisterUseCase.RegisterCommand(
                "banned@ordago.dev",
                "Password123!",
                "BannedUser",
                "01",
                "dev-1",
                "PC",
                "127.0.0.1"
        ));

        Account account = accountRepository.findByEmail("banned@ordago.dev").orElseThrow();
        Account bannedAccount = new Account(
                account.getId(),
                account.getEmail(),
                account.getPasswordHash(),
                account.isSystemAdmin(),
                account.isEmailVerified(),
                account.getEmailVerificationToken(),
                account.getEmailVerificationExpiresAt(),
                account.getPasswordResetToken(),
                account.getPasswordResetExpiresAt(),
                0,
                null,
                null,
                null,
                false,
                null,
                true,
                Instant.now().plusSeconds(3600),
                "Cheating violation",
                account.getCreatedAt(),
                account.getUpdatedAt(),
                null
        );
        accountRepository.save(bannedAccount);

        assertThatThrownBy(() -> authService.login(new LoginUseCase.LoginCommand(
                "banned@ordago.dev",
                "Password123!",
                "device-1",
                "PC",
                "127.0.0.1"
        ))).isInstanceOf(AccountBannedException.class);
    }

    @Test
    void shouldRotateRefreshTokenSuccessfully() {
        RegisterUseCase.AuthResult reg = authService.register(new RegisterUseCase.RegisterCommand(
                "refresh@ordago.dev",
                "Password!",
                "RefreshUser",
                "01",
                "dev-1",
                "PC",
                "127.0.0.1"
        ));

        String firstRefreshToken = reg.tokens().refreshToken();

        AuthTokens rotated = authService.refreshToken(new RefreshTokenUseCase.RefreshTokenCommand(
                firstRefreshToken,
                "127.0.0.1"
        ));

        assertThat(rotated.refreshToken()).isNotEqualTo(firstRefreshToken);

        UserSession session = sessionRepository.findById(reg.session().getId()).orElseThrow();
        assertThat(session.getPreviousRefreshTokenHash()).isEqualTo(tokenProvider.hashToken(firstRefreshToken));
        assertThat(session.getRefreshTokenHash()).isEqualTo(tokenProvider.hashToken(rotated.refreshToken()));
    }

    @Test
    void shouldAllowPreviousRefreshTokenWithinThirtySecondsGraceWindow() {
        RegisterUseCase.AuthResult reg = authService.register(new RegisterUseCase.RegisterCommand(
                "grace@ordago.dev",
                "Password!",
                "GraceUser",
                "01",
                "dev-1",
                "PC",
                "127.0.0.1"
        ));

        String initialToken = reg.tokens().refreshToken();
        authService.refreshToken(new RefreshTokenUseCase.RefreshTokenCommand(initialToken, "127.0.0.1"));

        // Presenting the previous token immediately (within 30s) succeeds (network retry simulation)
        AuthTokens retryTokens = authService.refreshToken(new RefreshTokenUseCase.RefreshTokenCommand(initialToken, "127.0.0.1"));
        assertThat(retryTokens).isNotNull();

        // Invalidate previous token so it cannot be replayed again
        assertThatThrownBy(() -> authService.refreshToken(new RefreshTokenUseCase.RefreshTokenCommand(initialToken, "127.0.0.1")))
                .isInstanceOf(SessionNotFoundException.class);
    }

    @Test
    void shouldRevokeSessionOnTokenTheftWhenPreviousTokenPresentedAfterGraceWindow() {
        RegisterUseCase.AuthResult reg = authService.register(new RegisterUseCase.RegisterCommand(
                "theft@ordago.dev",
                "Password!",
                "TheftUser",
                "01",
                "dev-1",
                "PC",
                "127.0.0.1"
        ));

        String stolenToken = reg.tokens().refreshToken();
        // Legitimate user refreshes token
        authService.refreshToken(new RefreshTokenUseCase.RefreshTokenCommand(stolenToken, "127.0.0.1"));

        // Simulate passage of 31 seconds
        UserSession session = sessionRepository.findById(reg.session().getId()).orElseThrow();
        session.setLastRefreshedAt(Instant.now().minusSeconds(31));
        sessionRepository.save(session);

        // Attacker presents the old stolen token after grace window
        assertThatThrownBy(() -> authService.refreshToken(new RefreshTokenUseCase.RefreshTokenCommand(stolenToken, "attacker-ip")))
                .isInstanceOf(SessionRevokedException.class);

        // Session must be marked revoked
        UserSession revokedSession = sessionRepository.findById(reg.session().getId()).orElseThrow();
        assertThat(revokedSession.isRevoked()).isTrue();
        assertThat(revokedSession.getRevocationReason()).isEqualTo("TOKEN_THEFT_DETECTED");
    }

    @Test
    void shouldLogoutAndRevokeSession() {
        RegisterUseCase.AuthResult reg = authService.register(new RegisterUseCase.RegisterCommand(
                "logout@ordago.dev",
                "Password!",
                "LogoutUser",
                "01",
                "dev-1",
                "PC",
                "127.0.0.1"
        ));

        authService.logout(new LogoutUseCase.LogoutCommand(reg.tokens().refreshToken(), null));

        UserSession session = sessionRepository.findById(reg.session().getId()).orElseThrow();
        assertThat(session.isRevoked()).isTrue();
        assertThat(session.getRevocationReason()).isEqualTo("USER_LOGOUT");
    }

    @Test
    void shouldEmitVerificationEmailOnRegistration() {
        sendEmailUseCase.sentEmails.clear();

        RegisterUseCase.AuthResult reg = authService.register(new RegisterUseCase.RegisterCommand(
                "verify_event@ordago.dev",
                "Password123!",
                "VerifyUser",
                "01",
                "dev-1",
                "PC",
                "127.0.0.1",
                "es"
        ));

        assertThat(sendEmailUseCase.sentEmails).hasSize(1);
        SendEmailCommand emailCmd = sendEmailUseCase.sentEmails.getFirst();
        assertThat(emailCmd.recipient()).isEqualTo("verify_event@ordago.dev");
        assertThat(emailCmd.template()).isEqualTo("email-verification");
        assertThat(emailCmd.templateData()).containsKey("verificationUrl");
        assertThat(emailCmd.templateData().get("verificationUrl")).isEqualTo(reg.account().getEmailVerificationToken());
        assertThat(emailCmd.language()).isEqualTo("es");
    }

    @Test
    void shouldVerifyEmailSuccessfully() {
        RegisterUseCase.AuthResult reg = authService.register(new RegisterUseCase.RegisterCommand(
                "verify_ok@ordago.dev",
                "Password123!",
                "VerifyOkUser",
                "01",
                "dev-1",
                "PC",
                "127.0.0.1"
        ));

        String token = reg.account().getEmailVerificationToken();
        assertThat(token).isNotBlank();

        authService.verifyEmail(token);

        Account account = accountRepository.findById(reg.account().getId()).orElseThrow();
        assertThat(account.isEmailVerified()).isTrue();
        assertThat(account.getEmailVerificationToken()).isNull();
        assertThat(account.getEmailVerificationExpiresAt()).isNull();
    }

    @Test
    void shouldRejectVerifyEmailWithInvalidToken() {
        assertThatThrownBy(() -> authService.verifyEmail("non-existent-token"))
                .isInstanceOf(InvalidVerificationTokenException.class);
        assertThatThrownBy(() -> authService.verifyEmail(null))
                .isInstanceOf(InvalidVerificationTokenException.class);
        assertThatThrownBy(() -> authService.verifyEmail("   "))
                .isInstanceOf(InvalidVerificationTokenException.class);
    }

    @Test
    void shouldRejectVerifyEmailWithExpiredToken() {
        UUID accountId = UUID.randomUUID();
        Account account = new Account(
                accountId,
                Email.of("expired_verify_svc@ordago.dev"),
                PasswordHash.of("hash"),
                false,
                false,
                "expired-token-123",
                Instant.now().minusSeconds(100),
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
        accountRepository.save(account);

        assertThatThrownBy(() -> authService.verifyEmail("expired-token-123"))
                .isInstanceOf(VerificationTokenExpiredException.class);
    }

    @Test
    void shouldResendVerificationEmailForUnverifiedAccount() {
        authService.register(new RegisterUseCase.RegisterCommand(
                "resend_test@ordago.dev",
                "Password123!",
                "ResendUser",
                "01",
                "dev-1",
                "PC",
                "127.0.0.1"
        ));

        sendEmailUseCase.sentEmails.clear();

        authService.resendVerificationEmail("resend_test@ordago.dev", "es");

        assertThat(sendEmailUseCase.sentEmails).hasSize(1);
        SendEmailCommand emailCmd = sendEmailUseCase.sentEmails.getFirst();
        assertThat(emailCmd.recipient()).isEqualTo("resend_test@ordago.dev");
        assertThat(emailCmd.template()).isEqualTo("email-verification");
    }

    @Test
    void shouldIgnoreResendVerificationForVerifiedAccount() {
        RegisterUseCase.AuthResult reg = authService.register(new RegisterUseCase.RegisterCommand(
                "already_verified@ordago.dev",
                "Password123!",
                "VerifiedUser",
                "01",
                "dev-1",
                "PC",
                "127.0.0.1"
        ));

        authService.verifyEmail(reg.account().getEmailVerificationToken());
        sendEmailUseCase.sentEmails.clear();

        authService.resendVerificationEmail("already_verified@ordago.dev", "es");

        assertThat(sendEmailUseCase.sentEmails).isEmpty();
    }

    @Test
    void shouldIgnoreResendVerificationForNonExistentEmail() {
        sendEmailUseCase.sentEmails.clear();

        authService.resendVerificationEmail("unknown_user@ordago.dev", "es");

        assertThat(sendEmailUseCase.sentEmails).isEmpty();
    }

    @Test
    void shouldRequestPasswordResetForActiveAccount() {
        authService.register(new RegisterUseCase.RegisterCommand(
                "forgot_pw@ordago.dev",
                "Password123!",
                "ForgotPwUser",
                "01",
                "dev-1",
                "PC",
                "127.0.0.1"
        ));

        sendEmailUseCase.sentEmails.clear();

        authService.requestPasswordReset("forgot_pw@ordago.dev", "es");

        assertThat(sendEmailUseCase.sentEmails).hasSize(1);
        SendEmailCommand emailCmd = sendEmailUseCase.sentEmails.getFirst();
        assertThat(emailCmd.recipient()).isEqualTo("forgot_pw@ordago.dev");
        assertThat(emailCmd.template()).isEqualTo("password-reset");
        assertThat(emailCmd.templateData()).containsKey("resetUrl");

        Account account = accountRepository.findByEmail("forgot_pw@ordago.dev").orElseThrow();
        assertThat(account.getPasswordResetToken()).isNotBlank();
        assertThat(account.getPasswordResetExpiresAt()).isAfter(Instant.now());
    }

    @Test
    void shouldIgnorePasswordResetForNonExistentEmail() {
        sendEmailUseCase.sentEmails.clear();

        authService.requestPasswordReset("does_not_exist@ordago.dev", "es");

        assertThat(sendEmailUseCase.sentEmails).isEmpty();
    }

    @Test
    void shouldIgnorePasswordResetForBannedAccount() {
        RegisterUseCase.AuthResult reg = authService.register(new RegisterUseCase.RegisterCommand(
                "banned_reset@ordago.dev",
                "Password123!",
                "BannedResetUser",
                "01",
                "dev-1",
                "PC",
                "127.0.0.1"
        ));

        Account account = accountRepository.findById(reg.account().getId()).orElseThrow();
        Account banned = new Account(
                account.getId(),
                account.getEmail(),
                account.getPasswordHash(),
                account.isSystemAdmin(),
                account.isEmailVerified(),
                account.getEmailVerificationToken(),
                account.getEmailVerificationExpiresAt(),
                account.getPasswordResetToken(),
                account.getPasswordResetExpiresAt(),
                0,
                null,
                null,
                null,
                false,
                null,
                true,
                Instant.now().plusSeconds(7200),
                "Suspended",
                account.getCreatedAt(),
                account.getUpdatedAt(),
                null
        );
        accountRepository.save(banned);
        sendEmailUseCase.sentEmails.clear();

        authService.requestPasswordReset("banned_reset@ordago.dev", "es");

        assertThat(sendEmailUseCase.sentEmails).isEmpty();
    }

    @Test
    void shouldResetPasswordSuccessfullyAndRevokeActiveSessions() {
        RegisterUseCase.AuthResult reg = authService.register(new RegisterUseCase.RegisterCommand(
                "reset_test@ordago.dev",
                "OldPassword123!",
                "ResetTestUser",
                "01",
                "dev-1",
                "PC",
                "127.0.0.1"
        ));

        authService.requestPasswordReset("reset_test@ordago.dev", "es");
        Account account = accountRepository.findById(reg.account().getId()).orElseThrow();
        String resetToken = account.getPasswordResetToken();

        authService.resetPassword(resetToken, "NewSecurePassword456!");

        // Verify old session revoked
        UserSession session = sessionRepository.findById(reg.session().getId()).orElseThrow();
        assertThat(session.isRevoked()).isTrue();
        assertThat(session.getRevocationReason()).isEqualTo("Password reset");

        // Verify login works with new password
        AuthResult loginResult = authService.login(new LoginUseCase.LoginCommand(
                "reset_test@ordago.dev",
                "NewSecurePassword456!",
                "dev-2",
                "Phone",
                "127.0.0.1"
        ));
        assertThat(loginResult).isNotNull();

        // Verify login fails with old password
        assertThatThrownBy(() -> authService.login(new LoginUseCase.LoginCommand(
                "reset_test@ordago.dev",
                "OldPassword123!",
                "dev-3",
                "Tablet",
                "127.0.0.1"
        ))).isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void shouldRejectResetPasswordWithInvalidToken() {
        assertThatThrownBy(() -> authService.resetPassword("invalid-token", "NewPass123!"))
                .isInstanceOf(InvalidPasswordResetTokenException.class);
        assertThatThrownBy(() -> authService.resetPassword(null, "NewPass123!"))
                .isInstanceOf(InvalidPasswordResetTokenException.class);
        assertThatThrownBy(() -> authService.resetPassword("   ", "NewPass123!"))
                .isInstanceOf(InvalidPasswordResetTokenException.class);
    }

    @Test
    void shouldRejectResetPasswordWithExpiredToken() {
        UUID accountId = UUID.randomUUID();
        Account account = new Account(
                accountId,
                Email.of("expired_pw_reset@ordago.dev"),
                PasswordHash.of("hash"),
                false,
                false,
                null,
                null,
                "expired-reset-token-123",
                Instant.now().minusSeconds(100),
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
        accountRepository.save(account);

        assertThatThrownBy(() -> authService.resetPassword("expired-reset-token-123", "NewPassword123!"))
                .isInstanceOf(PasswordResetTokenExpiredException.class);
    }

    @Test
    void shouldChangePasswordForAuthenticatedUserAndRevokeSessions() {
        RegisterUseCase.AuthResult reg = authService.register(new RegisterUseCase.RegisterCommand(
                "change_test@ordago.dev",
                "CurrentPass123!",
                "ChangeTestUser",
                "01",
                "dev-1",
                "PC",
                "127.0.0.1"
        ));

        authService.changePassword(reg.account().getId(), "CurrentPass123!", "NewChangedPass456!");

        // Verify session revoked
        UserSession session = sessionRepository.findById(reg.session().getId()).orElseThrow();
        assertThat(session.isRevoked()).isTrue();
        assertThat(session.getRevocationReason()).isEqualTo("Password changed");

        // Verify login with new password succeeds
        AuthResult loginResult = authService.login(new LoginUseCase.LoginCommand(
                "change_test@ordago.dev",
                "NewChangedPass456!",
                "dev-2",
                "Phone",
                "127.0.0.1"
        ));
        assertThat(loginResult).isNotNull();

        // Verify login with old password fails
        assertThatThrownBy(() -> authService.login(new LoginUseCase.LoginCommand(
                "change_test@ordago.dev",
                "CurrentPass123!",
                "dev-3",
                "Tablet",
                "127.0.0.1"
        ))).isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void shouldRejectChangePasswordWithIncorrectCurrentPassword() {
        RegisterUseCase.AuthResult reg = authService.register(new RegisterUseCase.RegisterCommand(
                "change_wrong@ordago.dev",
                "CorrectPass123!",
                "ChangeWrongUser",
                "01",
                "dev-1",
                "PC",
                "127.0.0.1"
        ));

        assertThatThrownBy(() -> authService.changePassword(
                reg.account().getId(),
                "IncorrectPass!",
                "NewPass123!"
        )).isInstanceOf(InvalidCredentialsException.class);
    }

    // --- In-Memory Test Doubles ---

    private static class InMemoryAccountRepository implements AccountRepositoryPort {
        private final Map<UUID, Account> accounts = new HashMap<>();

        @Override
        public Account save(Account account) {
            accounts.put(account.getId(), account);
            return account;
        }

        @Override
        public Optional<Account> findById(UUID id) {
            return Optional.ofNullable(accounts.get(id));
        }

        @Override
        public Optional<Account> findByEmail(String email) {
            return accounts.values().stream()
                    .filter(a -> a.getEmail().value().equalsIgnoreCase(email.trim().toLowerCase(Locale.ROOT)))
                    .findFirst();
        }

        @Override
        public boolean existsByEmail(String email) {
            return findByEmail(email).isPresent();
        }

        @Override
        public Optional<Account> findByEmailVerificationToken(String token) {
            return accounts.values().stream()
                    .filter(a -> token != null && token.equals(a.getEmailVerificationToken()))
                    .findFirst();
        }

        @Override
        public Optional<Account> findByPasswordResetToken(String token) {
            return accounts.values().stream()
                    .filter(a -> token != null && token.equals(a.getPasswordResetToken()))
                    .findFirst();
        }
    }

    private static class InMemoryUserSessionRepository implements UserSessionRepositoryPort {
        private final Map<UUID, UserSession> sessions = new HashMap<>();

        @Override
        public UserSession save(UserSession session) {
            sessions.put(session.getId(), session);
            return session;
        }

        @Override
        public Optional<UserSession> findById(UUID id) {
            return Optional.ofNullable(sessions.get(id));
        }

        @Override
        public Optional<UserSession> findByRefreshTokenHash(String refreshTokenHash) {
            return sessions.values().stream()
                    .filter(s -> refreshTokenHash.equals(s.getRefreshTokenHash()))
                    .findFirst();
        }

        @Override
        public Optional<UserSession> findByPreviousRefreshTokenHash(String previousRefreshTokenHash) {
            return sessions.values().stream()
                    .filter(s -> previousRefreshTokenHash.equals(s.getPreviousRefreshTokenHash()))
                    .findFirst();
        }

        @Override
        public Optional<UserSession> findByCurrentOrPreviousTokenHash(String tokenHash) {
            return sessions.values().stream()
                    .filter(s -> tokenHash.equals(s.getRefreshTokenHash()) || tokenHash.equals(s.getPreviousRefreshTokenHash()))
                    .findFirst();
        }

        @Override
        public List<UserSession> findActiveByAccountId(UUID accountId) {
            return sessions.values().stream()
                    .filter(s -> s.getAccountId().equals(accountId) && !s.isRevoked() && !s.isExpired())
                    .toList();
        }

        @Override
        public void revokeAllForAccount(UUID accountId, String reason, Instant revokedAt) {
            sessions.values().stream()
                    .filter(s -> s.getAccountId().equals(accountId))
                    .forEach(s -> s.revoke(reason));
        }
    }

    private static class StubPasswordEncoder implements PasswordEncoderPort {
        @Override
        public String encode(CharSequence rawPassword) {
            return "hashed-" + rawPassword;
        }

        @Override
        public boolean matches(CharSequence rawPassword, String encodedPassword) {
            return ("hashed-" + rawPassword).equals(encodedPassword);
        }
    }

    private static class StubTokenProvider implements TokenProviderPort {
        private int counter = 0;

        @Override
        public String createAccessToken(Account account, UserSession session) {
            return "access-token-" + account.getId();
        }

        @Override
        public String generateRefreshToken() {
            return "refresh-token-" + (++counter);
        }

        @Override
        public String hashToken(String token) {
            return "sha256-" + token;
        }

        @Override
        public long getAccessTokenLifespanSeconds() {
            return 900;
        }

        @Override
        public Duration getRefreshTokenLifespan() {
            return Duration.ofDays(30);
        }
    }

    private static class StubCreateUserProfileUseCase implements CreateUserProfileUseCase {
        private final Map<UUID, UserProfile> createdProfiles = new HashMap<>();

        @Override
        public UserProfile createUserProfile(CreateProfileCommand command) {
            PlayerTag playerTag = new PlayerTag(command.gameName(), command.tagLine());
            boolean alreadyExists = createdProfiles.values().stream()
                    .anyMatch(p -> p.getGameName().equalsIgnoreCase(playerTag.canonicalGameName())
                            && p.getTagLine().equalsIgnoreCase(playerTag.canonicalTagLine()));
            if (alreadyExists) {
                throw new PlayerTagAlreadyTakenException(command.gameName(), command.tagLine());
            }

            UserProfile profile = UserProfile.createDefault(
                    command.accountId(),
                    playerTag,
                    command.locale()
            );
            createdProfiles.put(command.accountId(), profile);
            return profile;
        }
    }

    private static class StubSendEmailUseCase implements SendEmailUseCase {
        private final List<SendEmailCommand> sentEmails = new ArrayList<>();

        @Override
        public void sendEmail(SendEmailCommand command) {
            sentEmails.add(command);
        }
    }
}
