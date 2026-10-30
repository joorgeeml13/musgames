package dev.ordago.auth.application;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
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
import dev.ordago.auth.domain.port.in.ChangePasswordUseCase;
import dev.ordago.auth.domain.port.in.GetCurrentAccountUseCase;
import dev.ordago.auth.domain.port.in.LoginUseCase;
import dev.ordago.auth.domain.port.in.LogoutUseCase;
import dev.ordago.auth.domain.port.in.RefreshTokenUseCase;
import dev.ordago.auth.domain.port.in.RegisterUseCase;
import dev.ordago.auth.domain.port.in.RequestPasswordResetUseCase;
import dev.ordago.auth.domain.port.in.ResendVerificationEmailUseCase;
import dev.ordago.auth.domain.port.in.ResetPasswordUseCase;
import dev.ordago.auth.domain.port.in.RevokeSessionUseCase;
import dev.ordago.auth.domain.port.in.VerifyEmailUseCase;
import dev.ordago.auth.domain.port.out.AccountRepositoryPort;
import dev.ordago.auth.domain.port.out.PasswordEncoderPort;
import dev.ordago.auth.domain.port.out.TokenProviderPort;
import dev.ordago.auth.domain.port.out.UserSessionRepositoryPort;
import dev.ordago.notification.domain.port.in.SendEmailUseCase;
import dev.ordago.notification.domain.port.in.SendEmailUseCase.SendEmailCommand;
import dev.ordago.profile.domain.port.in.CreateUserProfileUseCase;

@ApplicationScoped
public class AuthService implements
        RegisterUseCase,
        LoginUseCase,
        RefreshTokenUseCase,
        LogoutUseCase,
        RevokeSessionUseCase,
        GetCurrentAccountUseCase,
        VerifyEmailUseCase,
        ResendVerificationEmailUseCase,
        RequestPasswordResetUseCase,
        ResetPasswordUseCase,
        ChangePasswordUseCase {

    private static final String DUMMY_ARGON2_HASH =
            "$argon2id$v=19$m=65536,t=3,p=1$c29tZXNhbHQxMjM0NTY3OA$9S88u9pP7+h+K1lF8V9w2W2Vb5X3kZ6m0P1q2R3s4T4";

    private final AccountRepositoryPort accountRepository;
    private final UserSessionRepositoryPort userSessionRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final TokenProviderPort tokenProvider;
    private final CreateUserProfileUseCase createUserProfileUseCase;
    private final SendEmailUseCase sendEmailUseCase;

    private final int maxFailedAttempts;
    private final Duration lockDuration;
    private final int graceWindowSeconds;
    private final Duration verificationTokenDuration;
    private final Duration passwordResetTokenDuration;

    public AuthService(
            AccountRepositoryPort accountRepository,
            UserSessionRepositoryPort userSessionRepository,
            PasswordEncoderPort passwordEncoder,
            TokenProviderPort tokenProvider,
            CreateUserProfileUseCase createUserProfileUseCase,
            SendEmailUseCase sendEmailUseCase,
            @ConfigProperty(name = "ordago.auth.lockout.max-attempts", defaultValue = "5") int maxFailedAttempts,
            @ConfigProperty(name = "ordago.auth.lockout.duration-minutes", defaultValue = "15") int lockDurationMinutes,
            @ConfigProperty(name = "ordago.auth.refresh-token.grace-window-seconds", defaultValue = "30") int graceWindowSeconds,
            @ConfigProperty(name = "ordago.auth.verification-token.duration-hours", defaultValue = "24") int verificationTokenDurationHours,
            @ConfigProperty(name = "ordago.auth.password-reset-token.duration-minutes", defaultValue = "60") int passwordResetTokenDurationMinutes
    ) {
        this.accountRepository = Objects.requireNonNull(accountRepository, "AccountRepositoryPort cannot be null");
        this.userSessionRepository = Objects.requireNonNull(userSessionRepository, "UserSessionRepositoryPort cannot be null");
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder, "PasswordEncoderPort cannot be null");
        this.tokenProvider = Objects.requireNonNull(tokenProvider, "TokenProviderPort cannot be null");
        this.createUserProfileUseCase = Objects.requireNonNull(createUserProfileUseCase, "CreateUserProfileUseCase cannot be null");
        this.sendEmailUseCase = Objects.requireNonNull(sendEmailUseCase, "SendEmailUseCase cannot be null");
        this.maxFailedAttempts = maxFailedAttempts;
        this.lockDuration = Duration.ofMinutes(lockDurationMinutes);
        this.graceWindowSeconds = graceWindowSeconds;
        this.verificationTokenDuration = Duration.ofHours(verificationTokenDurationHours);
        this.passwordResetTokenDuration = Duration.ofMinutes(passwordResetTokenDurationMinutes);
    }

    @Override
    @Transactional
    public AuthResult register(RegisterCommand command) {
        Objects.requireNonNull(command, "RegisterCommand cannot be null");
        Email email = Email.of(command.email());

        if (accountRepository.existsByEmail(email.value())) {
            throw new EmailAlreadyExistsException(email.value());
        }

        PasswordHash passwordHash = PasswordHash.of(passwordEncoder.encode(command.password()));
        UUID accountId = UUID.randomUUID();
        Account account = Account.createNew(accountId, email, passwordHash);
        account.generateEmailVerificationToken(verificationTokenDuration);
        accountRepository.save(account);

        String tagLine = command.tagLine();
        if (tagLine == null || tagLine.isBlank()) {
            tagLine = generateDefaultTagLine();
        }

        createUserProfileUseCase.createUserProfile(
                new CreateUserProfileUseCase.CreateProfileCommand(
                        accountId,
                        command.gameName(),
                        tagLine,
                        "es-ES"
                )
        );

        SessionWithToken sessionWithToken = createSession(accountId, command.deviceId(), command.deviceName());
        userSessionRepository.save(sessionWithToken.session());

        String accessToken = tokenProvider.createAccessToken(account, sessionWithToken.session());
        AuthTokens tokens = AuthTokens.of(accessToken, sessionWithToken.plainRefreshToken(), tokenProvider.getAccessTokenLifespanSeconds());

        String language = (command.language() != null && !command.language().isBlank())
                ? command.language()
                : "es";
        sendEmailUseCase.sendEmail(
                SendEmailCommand.emailVerification(
                        account.getEmailValue(),
                        account.getEmailVerificationToken(),
                        language
                )
        );

        return new AuthResult(account, sessionWithToken.session(), tokens);
    }

    @Override
    @Transactional
    public AuthResult login(LoginCommand command) {
        Objects.requireNonNull(command, "LoginCommand cannot be null");
        Email email;
        try {
            email = Email.of(command.email());
        } catch (Exception e) {
            passwordEncoder.matches(command.password(), DUMMY_ARGON2_HASH);
            throw new InvalidCredentialsException();
        }

        Account account = accountRepository.findByEmail(email.value()).orElse(null);

        // Constant-time dummy verification when account does not exist to prevent timing attacks / user enumeration
        String hashToVerify = (account != null && !account.isDeleted())
                ? account.getPasswordHash().value()
                : DUMMY_ARGON2_HASH;
        boolean passwordMatches = passwordEncoder.matches(command.password(), hashToVerify);

        if (account == null || account.isDeleted()) {
            throw new InvalidCredentialsException();
        }

        if (account.getLockedUntil() != null && !account.isLocked()) {
            account.unlock();
        }

        if (!passwordMatches) {
            account.recordFailedLogin(maxFailedAttempts, lockDuration);
            accountRepository.save(account);

            if (account.isLocked()) {
                throw new AccountLockedException(account.getLockedUntil());
            }
            throw new InvalidCredentialsException();
        }

        // Credentials are valid; verify account administrative state
        if (account.isBanned()) {
            throw new AccountBannedException(account.getBanReason());
        }

        if (account.isLocked()) {
            throw new AccountLockedException(account.getLockedUntil());
        }

        account.recordSuccessfulLogin(command.clientIp());
        accountRepository.save(account);

        SessionWithToken sessionWithToken = createSession(account.getId(), command.deviceId(), command.deviceName());
        userSessionRepository.save(sessionWithToken.session());

        String accessToken = tokenProvider.createAccessToken(account, sessionWithToken.session());
        AuthTokens tokens = AuthTokens.of(accessToken, sessionWithToken.plainRefreshToken(), tokenProvider.getAccessTokenLifespanSeconds());
        return new AuthResult(account, sessionWithToken.session(), tokens);
    }

    @Override
    @Transactional
    public AuthTokens refreshToken(RefreshTokenCommand command) {
        Objects.requireNonNull(command, "RefreshTokenCommand cannot be null");
        String incomingTokenHash = tokenProvider.hashToken(command.refreshToken());

        UserSession session = userSessionRepository.findByCurrentOrPreviousTokenHash(incomingTokenHash)
                .orElseThrow(SessionNotFoundException::new);

        if (session.isRevoked()) {
            throw new SessionRevokedException(session.getRevocationReason());
        }

        if (session.isExpired()) {
            throw new SessionExpiredException();
        }

        // Check if rotated token used
        if (incomingTokenHash.equals(session.getPreviousRefreshTokenHash())) {
            if (!session.isWithinGraceWindow(graceWindowSeconds)) {
                // Theft detection: previous token used after grace window
                session.revoke("TOKEN_THEFT_DETECTED");
                userSessionRepository.save(session);
                throw new SessionRevokedException("auth.token_theft_detected");
            }

            // Valid network retry race condition: issue replacement and invalidate previous token to prevent sliding replay
            String newRefreshToken = tokenProvider.generateRefreshToken();
            String newRefreshTokenHash = tokenProvider.hashToken(newRefreshToken);
            session.setPreviousRefreshTokenHash(null);
            session.setRefreshTokenHash(newRefreshTokenHash);
            userSessionRepository.save(session);

            Account account = accountRepository.findById(session.getAccountId())
                    .orElseThrow(InvalidCredentialsException::new);
            String accessToken = tokenProvider.createAccessToken(account, session);
            return AuthTokens.of(accessToken, newRefreshToken, tokenProvider.getAccessTokenLifespanSeconds());
        }

        // Current refresh token presented: standard rotation
        String newRefreshToken = tokenProvider.generateRefreshToken();
        String newRefreshTokenHash = tokenProvider.hashToken(newRefreshToken);
        session.rotate(newRefreshTokenHash, tokenProvider.getRefreshTokenLifespan());
        userSessionRepository.save(session);

        Account account = accountRepository.findById(session.getAccountId())
                .orElseThrow(InvalidCredentialsException::new);
        String accessToken = tokenProvider.createAccessToken(account, session);
        return AuthTokens.of(accessToken, newRefreshToken, tokenProvider.getAccessTokenLifespanSeconds());
    }

    @Override
    @Transactional
    public void logout(LogoutCommand command) {
        if (command == null) {
            return;
        }
        if (command.refreshToken() != null && !command.refreshToken().isBlank()) {
            String hash = tokenProvider.hashToken(command.refreshToken());
            userSessionRepository.findByCurrentOrPreviousTokenHash(hash).ifPresent(session -> {
                session.revoke("USER_LOGOUT");
                userSessionRepository.save(session);
            });
        }
        if (command.sessionId() != null) {
            userSessionRepository.findById(command.sessionId()).ifPresent(session -> {
                session.revoke("USER_LOGOUT");
                userSessionRepository.save(session);
            });
        }
    }

    @Override
    @Transactional
    public void revokeSession(UUID sessionId, String reason) {
        Objects.requireNonNull(sessionId, "sessionId cannot be null");
        userSessionRepository.findById(sessionId).ifPresent(session -> {
            session.revoke(reason != null ? reason : "ADMIN_REVOKED");
            userSessionRepository.save(session);
        });
    }

    @Override
    @Transactional
    public void revokeAllSessionsForAccount(UUID accountId, String reason) {
        Objects.requireNonNull(accountId, "accountId cannot be null");
        userSessionRepository.revokeAllForAccount(accountId, reason != null ? reason : "ADMIN_REVOKED", Instant.now());
    }

    @Override
    @Transactional
    public Account getAccountById(UUID accountId) {
        Objects.requireNonNull(accountId, "accountId cannot be null");
        Account account = accountRepository.findById(accountId)
                .orElseThrow(InvalidCredentialsException::new);
        if (account.isDeleted()) {
            throw new InvalidCredentialsException();
        }
        return account;
    }

    @Override
    @Transactional
    public void verifyEmail(String token) {
        if (token == null || token.isBlank()) {
            throw new InvalidVerificationTokenException();
        }
        Account account = accountRepository.findByEmailVerificationToken(token)
                .orElseThrow(InvalidVerificationTokenException::new);
        account.verifyEmail(token);
        accountRepository.save(account);
    }

    @Override
    @Transactional
    public void resendVerificationEmail(String email, String language) {
        if (email == null || email.isBlank()) {
            return;
        }
        Email emailVo;
        try {
            emailVo = Email.of(email);
        } catch (Exception e) {
            return;
        }
        Optional<Account> accountOpt = accountRepository.findByEmail(emailVo.value());
        if (accountOpt.isEmpty()) {
            return;
        }
        Account account = accountOpt.get();
        if (account.isDeleted() || account.isEmailVerified()) {
            return;
        }
        account.generateEmailVerificationToken(verificationTokenDuration);
        accountRepository.save(account);

        String resolvedLanguage = (language != null && !language.isBlank()) ? language : "es";
        sendEmailUseCase.sendEmail(
                SendEmailCommand.emailVerification(
                        account.getEmailValue(),
                        account.getEmailVerificationToken(),
                        resolvedLanguage
                )
        );
    }

    @Override
    @Transactional
    public void requestPasswordReset(String email, String language) {
        if (email == null || email.isBlank()) {
            return;
        }
        Email emailVo;
        try {
            emailVo = Email.of(email);
        } catch (Exception e) {
            return;
        }
        Optional<Account> accountOpt = accountRepository.findByEmail(emailVo.value());
        if (accountOpt.isEmpty()) {
            return;
        }
        Account account = accountOpt.get();
        if (account.isDeleted() || account.isBanned()) {
            return;
        }
        account.generatePasswordResetToken(passwordResetTokenDuration);
        accountRepository.save(account);

        String resolvedLanguage = (language != null && !language.isBlank()) ? language : "es";
        sendEmailUseCase.sendEmail(
                SendEmailCommand.passwordReset(
                        account.getEmailValue(),
                        account.getPasswordResetToken(),
                        resolvedLanguage
                )
        );
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
        if (token == null || token.isBlank()) {
            throw new InvalidPasswordResetTokenException();
        }
        Account account = accountRepository.findByPasswordResetToken(token)
                .orElseThrow(InvalidPasswordResetTokenException::new);

        PasswordHash newPasswordHash = PasswordHash.of(passwordEncoder.encode(newPassword));
        account.resetPassword(token, newPasswordHash);
        accountRepository.save(account);

        userSessionRepository.revokeAllForAccount(account.getId(), "Password reset", Instant.now());
    }

    @Override
    @Transactional
    public void changePassword(UUID accountId, String currentPassword, String newPassword) {
        Objects.requireNonNull(accountId, "accountId cannot be null");
        Account account = accountRepository.findById(accountId)
                .orElseThrow(InvalidCredentialsException::new);
        if (account.isDeleted()) {
            throw new InvalidCredentialsException();
        }

        if (!passwordEncoder.matches(currentPassword, account.getPasswordHash().value())) {
            throw new InvalidCredentialsException();
        }

        PasswordHash newPasswordHash = PasswordHash.of(passwordEncoder.encode(newPassword));
        account.changePassword(newPasswordHash);
        accountRepository.save(account);

        userSessionRepository.revokeAllForAccount(accountId, "Password changed", Instant.now());
    }

    private record SessionWithToken(UserSession session, String plainRefreshToken) {}

    private SessionWithToken createSession(UUID accountId, String deviceId, String deviceName) {
        String plainRefreshToken = tokenProvider.generateRefreshToken();
        String refreshTokenHash = tokenProvider.hashToken(plainRefreshToken);
        UUID sessionId = UUID.randomUUID();
        String resolvedDeviceId = (deviceId != null && !deviceId.isBlank())
                ? deviceId
                : UUID.randomUUID().toString();

        UserSession session = UserSession.createNew(
                sessionId,
                accountId,
                resolvedDeviceId,
                deviceName,
                refreshTokenHash,
                tokenProvider.getRefreshTokenLifespan()
        );
        return new SessionWithToken(session, plainRefreshToken);
    }

    private String generateDefaultTagLine() {
        int randomNum = java.util.concurrent.ThreadLocalRandom.current().nextInt(1000, 9999);
        return String.valueOf(randomNum);
    }
}
