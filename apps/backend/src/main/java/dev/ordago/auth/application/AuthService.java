package dev.ordago.auth.application;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import io.quarkus.arc.Arc;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import dev.ordago.auth.domain.event.TokenTheftDetectedEvent;
import dev.ordago.auth.domain.exception.AccountBannedException;
import dev.ordago.auth.domain.exception.AccountLockedException;
import dev.ordago.auth.domain.exception.AccountNotGuestException;
import dev.ordago.auth.domain.exception.EmailAlreadyExistsException;
import dev.ordago.auth.domain.exception.InvalidCredentialsException;
import dev.ordago.auth.domain.exception.InvalidOAuthTokenException;
import dev.ordago.auth.domain.exception.InvalidPasswordResetTokenException;
import dev.ordago.auth.domain.exception.InvalidRefreshTokenException;
import dev.ordago.auth.domain.exception.InvalidVerificationTokenException;
import dev.ordago.auth.domain.exception.OAuthIdentityAlreadyLinkedException;
import dev.ordago.auth.domain.exception.PasswordResetTokenExpiredException;
import dev.ordago.auth.domain.exception.SessionExpiredException;
import dev.ordago.auth.domain.exception.SessionNotFoundException;
import dev.ordago.auth.domain.exception.SessionRevokedException;
import dev.ordago.auth.domain.exception.VerificationTokenExpiredException;
import dev.ordago.auth.domain.model.Account;
import dev.ordago.auth.domain.model.AccountIdentity;
import dev.ordago.auth.domain.model.AuthTokens;
import dev.ordago.auth.domain.model.Email;
import dev.ordago.auth.domain.model.OAuthUserInfo;
import dev.ordago.auth.domain.model.PasswordHash;
import dev.ordago.auth.domain.model.UserSession;
import dev.ordago.auth.domain.port.in.ChangePasswordUseCase;
import dev.ordago.auth.domain.port.in.ClaimGuestAccountUseCase;
import dev.ordago.auth.domain.port.in.ClaimGuestWithSocialUseCase;
import dev.ordago.auth.domain.port.in.GetCurrentAccountUseCase;
import dev.ordago.auth.domain.port.in.GetUserSessionsUseCase;
import dev.ordago.auth.domain.port.in.GuestLoginUseCase;
import dev.ordago.auth.domain.port.in.LoginUseCase;
import dev.ordago.auth.domain.port.in.LogoutUseCase;
import dev.ordago.auth.domain.port.in.PurgeExpiredGuestAccountsUseCase;
import dev.ordago.auth.domain.port.in.RefreshTokenUseCase;
import dev.ordago.auth.domain.port.in.RegisterUseCase;
import dev.ordago.auth.domain.port.in.RequestPasswordResetUseCase;
import dev.ordago.auth.domain.port.in.ResendVerificationEmailUseCase;
import dev.ordago.auth.domain.port.in.ResetPasswordUseCase;
import dev.ordago.auth.domain.port.in.RevokeSessionUseCase;
import dev.ordago.auth.domain.port.in.SocialLoginUseCase;
import dev.ordago.auth.domain.port.in.VerifyEmailUseCase;
import dev.ordago.auth.domain.port.out.AccountIdentityRepositoryPort;
import dev.ordago.auth.domain.port.out.AccountRepositoryPort;
import dev.ordago.auth.domain.port.out.OAuthVerifierPort;
import dev.ordago.auth.domain.port.out.PasswordEncoderPort;
import dev.ordago.auth.domain.port.out.TokenProviderPort;
import dev.ordago.auth.domain.port.out.UserSessionRepositoryPort;
import dev.ordago.notification.domain.port.in.SendEmailUseCase;
import dev.ordago.notification.domain.port.in.SendEmailUseCase.SendEmailCommand;
import dev.ordago.profile.domain.exception.UserProfileNotFoundException;
import dev.ordago.profile.domain.model.PlayerTag;
import dev.ordago.profile.domain.model.UserProfile;
import dev.ordago.profile.domain.port.in.CreateUserProfileUseCase;
import dev.ordago.profile.domain.port.in.GetUserProfileUseCase;
import dev.ordago.shared.domain.exception.ValidationException;
import dev.ordago.shared.domain.port.out.EventPublisherPort;

@ApplicationScoped
public class AuthService implements
        RegisterUseCase,
        LoginUseCase,
        RefreshTokenUseCase,
        LogoutUseCase,
        RevokeSessionUseCase,
        GetUserSessionsUseCase,
        GetCurrentAccountUseCase,
        VerifyEmailUseCase,
        ResendVerificationEmailUseCase,
        RequestPasswordResetUseCase,
        ResetPasswordUseCase,
        ChangePasswordUseCase,
        GuestLoginUseCase,
        ClaimGuestAccountUseCase,
        PurgeExpiredGuestAccountsUseCase,
        SocialLoginUseCase,
        ClaimGuestWithSocialUseCase {

    private static final String DUMMY_ARGON2_HASH =
            "$argon2id$v=19$m=65536,t=3,p=1$c29tZXNhbHQxMjM0NTY3OA$9S88u9pP7+h+K1lF8V9w2W2Vb5X3kZ6m0P1q2R3s4T4";
    private static final int MAX_RETENTION_DAYS = 3650;

    private final AccountRepositoryPort accountRepository;
    private final UserSessionRepositoryPort userSessionRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final TokenProviderPort tokenProvider;
    private final CreateUserProfileUseCase createUserProfileUseCase;
    private final GetUserProfileUseCase getUserProfileUseCase;
    private final SendEmailUseCase sendEmailUseCase;
    private final EventPublisherPort eventPublisherPort;
    private final AccountIdentityRepositoryPort accountIdentityRepository;
    private final OAuthVerifierPort oauthVerifierPort;

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
            GetUserProfileUseCase getUserProfileUseCase,
            SendEmailUseCase sendEmailUseCase,
            EventPublisherPort eventPublisherPort,
            AccountIdentityRepositoryPort accountIdentityRepository,
            OAuthVerifierPort oauthVerifierPort,
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
        this.getUserProfileUseCase = Objects.requireNonNull(getUserProfileUseCase, "GetUserProfileUseCase cannot be null");
        this.sendEmailUseCase = Objects.requireNonNull(sendEmailUseCase, "SendEmailUseCase cannot be null");
        this.eventPublisherPort = Objects.requireNonNull(eventPublisherPort, "EventPublisherPort cannot be null");
        this.accountIdentityRepository = Objects.requireNonNull(accountIdentityRepository, "AccountIdentityRepositoryPort cannot be null");
        this.oauthVerifierPort = Objects.requireNonNull(oauthVerifierPort, "OAuthVerifierPort cannot be null");
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
        PlayerTag.validateGameName(command.gameName());

        if (accountRepository.existsByEmail(email.value())) {
            throw new EmailAlreadyExistsException(email.value());
        }

        PasswordHash passwordHash = PasswordHash.of(passwordEncoder.encode(command.password()));
        UUID accountId = UUID.randomUUID();
        Account account = Account.createNew(accountId, email, passwordHash);
        String verificationToken = account.generateEmailVerificationToken(verificationTokenDuration);
        accountRepository.save(account);

        String tagLine = command.tagLine();
        if (tagLine == null || tagLine.isBlank()) {
            tagLine = generateDefaultTagLine(command.gameName());
        }

        UserProfile profile = createUserProfileUseCase.createUserProfile(
                new CreateUserProfileUseCase.CreateProfileCommand(
                        accountId,
                        command.gameName(),
                        tagLine,
                        "es-ES"
                )
        );

        SessionWithToken sessionWithToken = createSession(accountId, command.deviceId(), command.deviceName());
        userSessionRepository.save(sessionWithToken.session());

        String playerTag = (profile != null && profile.getPlayerTag() != null) ? profile.getPlayerTag().formatted() : null;
        String accessToken = tokenProvider.createAccessToken(account, sessionWithToken.session(), playerTag);
        AuthTokens tokens = AuthTokens.of(accessToken, sessionWithToken.plainRefreshToken(), tokenProvider.getAccessTokenLifespanSeconds());

        String language = (command.language() != null && !command.language().isBlank())
                ? command.language()
                : "es";
        sendEmailUseCase.sendEmail(
                SendEmailCommand.emailVerification(
                        account.getEmailValue(),
                        verificationToken,
                        language
                )
        );

        return new AuthResult(account, sessionWithToken.session(), tokens);
    }

    @Override
    @Transactional
    public AuthResult loginAsGuest(GuestLoginCommand command) {
        Objects.requireNonNull(command, "GuestLoginCommand cannot be null");
        UUID accountId = UUID.randomUUID();
        Account account = Account.createGuest(accountId, Instant.now());
        accountRepository.save(account);

        UserProfile profile = null;
        for (int i = 0; i < 5; i++) {
            int randomDigits = ThreadLocalRandom.current().nextInt(1000, 10000);
            String guestGameName = "Guest" + randomDigits;
            try {
                profile = createUserProfileUseCase.createUserProfile(
                        new CreateUserProfileUseCase.CreateProfileCommand(
                                accountId,
                                guestGameName,
                                "666",
                                "es-ES"
                        )
                );
                break;
            } catch (Exception e) {
                if (i == 4) {
                    throw e;
                }
            }
        }

        SessionWithToken sessionWithToken = createSession(accountId, command.deviceId(), command.deviceName());
        userSessionRepository.save(sessionWithToken.session());

        String playerTag = (profile != null && profile.getPlayerTag() != null) ? profile.getPlayerTag().formatted() : null;
        String accessToken = tokenProvider.createAccessToken(account, sessionWithToken.session(), playerTag);
        AuthTokens tokens = AuthTokens.of(accessToken, sessionWithToken.plainRefreshToken(), tokenProvider.getAccessTokenLifespanSeconds());

        return new AuthResult(account, sessionWithToken.session(), tokens);
    }

    @Override
    @Transactional
    public AuthResult claimGuestAccount(ClaimGuestCommand command) {
        Objects.requireNonNull(command, "ClaimGuestCommand cannot be null");
        Objects.requireNonNull(command.accountId(), "accountId cannot be null");
        Email email = Email.of(command.email());

        Account account = accountRepository.findById(command.accountId())
                .orElseThrow(InvalidCredentialsException::new);

        if (account.isDeleted()) {
            throw new InvalidCredentialsException();
        }

        if (account.isBanned()) {
            throw new AccountBannedException(account.getBanReason());
        }

        if (!account.isGuest()) {
            throw new AccountNotGuestException();
        }

        if (accountRepository.findByEmail(email.value()).isPresent()) {
            throw new EmailAlreadyExistsException(email.value());
        }

        PasswordHash passwordHash = PasswordHash.of(passwordEncoder.encode(command.password()));
        String verificationToken = account.claimAsRegular(email, passwordHash, verificationTokenDuration);
        accountRepository.save(account);

        userSessionRepository.revokeAllForAccount(account.getId(), "Account claimed as regular", Instant.now());

        String language = (command.language() != null && !command.language().isBlank())
                ? command.language()
                : "es";
        sendEmailUseCase.sendEmail(
                SendEmailCommand.emailVerification(
                        account.getEmailValue(),
                        verificationToken,
                        language
                )
        );

        SessionWithToken sessionWithToken = createSession(account.getId(), command.deviceId(), command.deviceName());
        userSessionRepository.save(sessionWithToken.session());

        String playerTag = resolvePlayerTag(account.getId());
        String accessToken = tokenProvider.createAccessToken(account, sessionWithToken.session(), playerTag);
        AuthTokens tokens = AuthTokens.of(accessToken, sessionWithToken.plainRefreshToken(), tokenProvider.getAccessTokenLifespanSeconds());

        return new AuthResult(account, sessionWithToken.session(), tokens);
    }

    @Override
    @Transactional
    public PurgeGuestsResult purgeExpiredGuestAccounts(int retentionDays) {
        if (retentionDays < 0 || retentionDays > MAX_RETENTION_DAYS) {
            throw new ValidationException("VALIDATION_ERROR", "Retention days must be between 0 and " + MAX_RETENTION_DAYS + ": " + retentionDays);
        }

        Instant cutoff = Instant.now().minus(Duration.ofDays(retentionDays));
        long purgedCount = accountRepository.deleteGuestsOlderThan(cutoff);

        return new PurgeGuestsResult(purgedCount, cutoff, retentionDays);
    }

    @Override
    @Transactional(dontRollbackOn = InvalidCredentialsException.class)
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
        String hashToVerify = (account != null && !account.isDeleted() && account.getPasswordHashValue() != null)
                ? account.getPasswordHashValue()
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

        String playerTag = resolvePlayerTag(account.getId());
        String accessToken = tokenProvider.createAccessToken(account, sessionWithToken.session(), playerTag);
        AuthTokens tokens = AuthTokens.of(accessToken, sessionWithToken.plainRefreshToken(), tokenProvider.getAccessTokenLifespanSeconds());
        return new AuthResult(account, sessionWithToken.session(), tokens);
    }

    @Override
    @Transactional(dontRollbackOn = {
            SessionRevokedException.class,
            InvalidCredentialsException.class,
            AccountBannedException.class
    })
    public AuthTokens refreshToken(RefreshTokenCommand command) {
        Objects.requireNonNull(command, "RefreshTokenCommand cannot be null");
        String incomingTokenHash = tokenProvider.hashToken(command.refreshToken());

        UserSession session = userSessionRepository.findByCurrentOrPreviousTokenHash(incomingTokenHash)
                .orElseThrow(InvalidRefreshTokenException::new);

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
                eventPublisherPort.publish(TokenTheftDetectedEvent.of(
                        session.getAccountId(),
                        session.getId(),
                        session.getDeviceId(),
                        session.getDeviceName(),
                        command.clientIp()
                ));
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

            if (account.isDeleted()) {
                session.revoke("ACCOUNT_DELETED");
                userSessionRepository.save(session);
                throw new InvalidCredentialsException();
            }

            if (account.isBanned()) {
                session.revoke("ACCOUNT_BANNED");
                userSessionRepository.save(session);
                throw new AccountBannedException(account.getBanReason());
            }

            String playerTag = resolvePlayerTag(account.getId());
            String accessToken = tokenProvider.createAccessToken(account, session, playerTag);
            return AuthTokens.of(accessToken, newRefreshToken, tokenProvider.getAccessTokenLifespanSeconds());
        }

        // Current refresh token presented: standard rotation
        String newRefreshToken = tokenProvider.generateRefreshToken();
        String newRefreshTokenHash = tokenProvider.hashToken(newRefreshToken);
        session.rotate(newRefreshTokenHash, tokenProvider.getRefreshTokenLifespan());
        userSessionRepository.save(session);

        Account account = accountRepository.findById(session.getAccountId())
                .orElseThrow(InvalidCredentialsException::new);

        if (account.isDeleted()) {
            session.revoke("ACCOUNT_DELETED");
            userSessionRepository.save(session);
            throw new InvalidCredentialsException();
        }

        if (account.isBanned()) {
            session.revoke("ACCOUNT_BANNED");
            userSessionRepository.save(session);
            throw new AccountBannedException(account.getBanReason());
        }

        String playerTag = resolvePlayerTag(account.getId());
        String accessToken = tokenProvider.createAccessToken(account, session, playerTag);
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
    public List<UserSession> getActiveSessions(UUID accountId) {
        Objects.requireNonNull(accountId, "accountId cannot be null");
        return userSessionRepository.findActiveByAccountId(accountId);
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
    public void revokeSession(UUID accountId, UUID sessionId, String reason) {
        Objects.requireNonNull(accountId, "accountId cannot be null");
        Objects.requireNonNull(sessionId, "sessionId cannot be null");
        UserSession session = userSessionRepository.findById(sessionId)
                .orElseThrow(SessionNotFoundException::new);
        if (!session.getAccountId().equals(accountId)) {
            throw new SessionNotFoundException();
        }
        session.revoke(reason != null ? reason : "USER_MANUAL_REVOCATION");
        userSessionRepository.save(session);
    }

    @Override
    @Transactional
    public void revokeOtherSessions(UUID accountId, UUID excludedSessionId, String reason) {
        Objects.requireNonNull(accountId, "accountId cannot be null");
        Objects.requireNonNull(excludedSessionId, "excludedSessionId cannot be null");
        userSessionRepository.revokeAllExcept(
                accountId,
                excludedSessionId,
                reason != null ? reason : "USER_REVOKED_OTHER_DEVICES",
                Instant.now()
        );
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
        String tokenHash = tokenProvider.hashToken(token.trim());
        Account account = accountRepository.findByEmailVerificationTokenHash(tokenHash)
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
        String verificationToken = account.generateEmailVerificationToken(verificationTokenDuration);
        accountRepository.save(account);

        String resolvedLanguage = (language != null && !language.isBlank()) ? language : "es";
        sendEmailUseCase.sendEmail(
                SendEmailCommand.emailVerification(
                        account.getEmailValue(),
                        verificationToken,
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
        String resetToken = account.generatePasswordResetToken(passwordResetTokenDuration);
        accountRepository.save(account);

        String resolvedLanguage = (language != null && !language.isBlank()) ? language : "es";
        sendEmailUseCase.sendEmail(
                SendEmailCommand.passwordReset(
                        account.getEmailValue(),
                        resetToken,
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
        String tokenHash = tokenProvider.hashToken(token.trim());
        Account account = accountRepository.findByPasswordResetTokenHash(tokenHash)
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

        if (account.getPasswordHashValue() == null || !passwordEncoder.matches(currentPassword, account.getPasswordHashValue())) {
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

    private String generateDefaultTagLine(String gameName) {
        return createUserProfileUseCase.calculateAvailableTagLine(gameName);
    }

    private <T> T runInTransaction(Callable<T> action) {
        if (Arc.container() != null) {
            return QuarkusTransaction.requiringNew().call(action);
        }
        try {
            return action.call();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public AuthResult socialLogin(SocialLoginCommand command) {
        Objects.requireNonNull(command, "SocialLoginCommand cannot be null");
        OAuthUserInfo userInfo = oauthVerifierPort.verify(command.provider(), command.token());

        if (userInfo.email() == null || userInfo.email().isBlank() || !userInfo.emailVerified()) {
            throw new InvalidOAuthTokenException();
        }

        return runInTransaction(() -> executeSocialLogin(command, userInfo));
    }

    private AuthResult executeSocialLogin(SocialLoginCommand command, OAuthUserInfo userInfo) {
        // 2. Check if identity already exists
        Optional<AccountIdentity> identityOpt = accountIdentityRepository.findByProviderAndSubject(
                command.provider(),
                userInfo.providerSubject()
        );

        if (identityOpt.isPresent()) {
            AccountIdentity identity = identityOpt.get();
            Account account = accountRepository.findById(identity.accountId())
                    .orElseThrow(InvalidCredentialsException::new);

            if (account.isDeleted()) {
                throw new InvalidCredentialsException();
            }

            if (account.isBanned()) {
                throw new AccountBannedException(account.getBanReason());
            }

            account.recordSuccessfulLogin(command.clientIp());
            accountRepository.save(account);

            SessionWithToken sessionWithToken = createSession(account.getId(), command.deviceId(), command.deviceName());
            userSessionRepository.save(sessionWithToken.session());

            String playerTag = resolvePlayerTag(account.getId());
            String accessToken = tokenProvider.createAccessToken(account, sessionWithToken.session(), playerTag);
            AuthTokens tokens = AuthTokens.of(accessToken, sessionWithToken.plainRefreshToken(), tokenProvider.getAccessTokenLifespanSeconds());
            return new AuthResult(account, sessionWithToken.session(), tokens);
        }

        // 3. Identity does not exist, but userInfo.email() matches an existing account
        if (userInfo.email() != null && !userInfo.email().isBlank()) {
            Optional<Account> existingAccountOpt = accountRepository.findByEmail(userInfo.email());
            if (existingAccountOpt.isPresent()) {
                Account account = existingAccountOpt.get();
                if (account.isDeleted()) {
                    throw new InvalidCredentialsException();
                }
                if (account.isBanned()) {
                    throw new AccountBannedException(account.getBanReason());
                }

                AccountIdentity newIdentity = AccountIdentity.createNew(
                        UUID.randomUUID(),
                        account.getId(),
                        command.provider(),
                        userInfo.providerSubject(),
                        userInfo.email(),
                        Instant.now()
                );
                accountIdentityRepository.save(newIdentity);

                if (!account.isEmailVerified() && userInfo.emailVerified()) {
                    account.markEmailVerified();
                }

                account.recordSuccessfulLogin(command.clientIp());
                accountRepository.save(account);

                SessionWithToken sessionWithToken = createSession(account.getId(), command.deviceId(), command.deviceName());
                userSessionRepository.save(sessionWithToken.session());

                String playerTag = resolvePlayerTag(account.getId());
                String accessToken = tokenProvider.createAccessToken(account, sessionWithToken.session(), playerTag);
                AuthTokens tokens = AuthTokens.of(accessToken, sessionWithToken.plainRefreshToken(), tokenProvider.getAccessTokenLifespanSeconds());
                return new AuthResult(account, sessionWithToken.session(), tokens);
            }
        }

        // 4. Create new account, identity, user profile, and session
        UUID accountId = UUID.randomUUID();
        Account account = Account.createSocial(accountId, Email.of(userInfo.email()), Instant.now());
        account.recordSuccessfulLogin(command.clientIp());
        accountRepository.save(account);

        AccountIdentity identity = AccountIdentity.createNew(
                UUID.randomUUID(),
                accountId,
                command.provider(),
                userInfo.providerSubject(),
                userInfo.email(),
                Instant.now()
        );
        accountIdentityRepository.save(identity);

        String gameName = sanitizeGameName(userInfo.displayName());
        String tagLine = createUserProfileUseCase.calculateAvailableTagLine(gameName);
        UserProfile profile = createUserProfileUseCase.createUserProfile(
                new CreateUserProfileUseCase.CreateProfileCommand(
                        accountId,
                        gameName,
                        tagLine,
                        "es-ES"
                )
        );

        SessionWithToken sessionWithToken = createSession(accountId, command.deviceId(), command.deviceName());
        userSessionRepository.save(sessionWithToken.session());

        String playerTag = (profile != null && profile.getPlayerTag() != null) ? profile.getPlayerTag().formatted() : null;
        String accessToken = tokenProvider.createAccessToken(account, sessionWithToken.session(), playerTag);
        AuthTokens tokens = AuthTokens.of(accessToken, sessionWithToken.plainRefreshToken(), tokenProvider.getAccessTokenLifespanSeconds());
        return new AuthResult(account, sessionWithToken.session(), tokens);
    }

    @Override
    public AuthResult claimGuestWithSocial(ClaimGuestSocialCommand command) {
        Objects.requireNonNull(command, "ClaimGuestSocialCommand cannot be null");
        Objects.requireNonNull(command.guestAccountId(), "guestAccountId cannot be null");

        OAuthUserInfo userInfo = oauthVerifierPort.verify(command.provider(), command.token());

        if (userInfo.email() == null || userInfo.email().isBlank() || !userInfo.emailVerified()) {
            throw new InvalidOAuthTokenException();
        }

        return runInTransaction(() -> executeClaimGuestWithSocial(command, userInfo));
    }

    private AuthResult executeClaimGuestWithSocial(ClaimGuestSocialCommand command, OAuthUserInfo userInfo) {
        Account account = accountRepository.findById(command.guestAccountId())
                .orElseThrow(InvalidCredentialsException::new);

        if (account.isDeleted()) {
            throw new InvalidCredentialsException();
        }

        if (account.isBanned()) {
            throw new AccountBannedException(account.getBanReason());
        }

        // 1. Validate guest
        if (!account.isGuest()) {
            throw new AccountNotGuestException();
        }

        // 2. Verify provider identity not linked to another user
        Optional<AccountIdentity> existingIdentity = accountIdentityRepository.findByProviderAndSubject(
                command.provider(),
                userInfo.providerSubject()
        );
        if (existingIdentity.isPresent()) {
            throw new OAuthIdentityAlreadyLinkedException();
        }

        // 3. Verify email does not collide with another registered account
        Optional<Account> existingAccountWithEmail = accountRepository.findByEmail(userInfo.email());
        if (existingAccountWithEmail.isPresent() && !existingAccountWithEmail.get().getId().equals(account.getId())) {
            throw new EmailAlreadyExistsException(userInfo.email());
        }

        // 4. Promote account to social
        account.claimAsSocial(Email.of(userInfo.email()));
        account.recordSuccessfulLogin(command.clientIp());
        accountRepository.save(account);

        // 5. Save new AccountIdentity
        AccountIdentity identity = AccountIdentity.createNew(
                UUID.randomUUID(),
                account.getId(),
                command.provider(),
                userInfo.providerSubject(),
                userInfo.email(),
                Instant.now()
        );
        accountIdentityRepository.save(identity);

        // 6. Revoke previous anonymous sessions, create new session and emit tokens
        userSessionRepository.revokeAllForAccount(account.getId(), "Account claimed with social", Instant.now());

        SessionWithToken sessionWithToken = createSession(account.getId(), command.deviceId(), command.deviceName());
        userSessionRepository.save(sessionWithToken.session());

        String playerTag = resolvePlayerTag(account.getId());
        String accessToken = tokenProvider.createAccessToken(account, sessionWithToken.session(), playerTag);
        AuthTokens tokens = AuthTokens.of(accessToken, sessionWithToken.plainRefreshToken(), tokenProvider.getAccessTokenLifespanSeconds());

        return new AuthResult(account, sessionWithToken.session(), tokens);
    }

    private String sanitizeGameName(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            return "Player";
        }
        String sanitized = displayName.replaceAll("[^a-zA-Z0-9_\\-\\s]", "").trim();
        sanitized = sanitized.replaceAll("\\s+", " ").trim();
        if (sanitized.length() < 2) {
            return "Player";
        }
        if (sanitized.length() > 24) {
            sanitized = sanitized.substring(0, 24).trim();
            if (sanitized.length() < 2) {
                return "Player";
            }
        }
        return sanitized;
    }

    private String resolvePlayerTag(UUID accountId) {
        if (accountId == null) {
            return null;
        }
        try {
            UserProfile profile = getUserProfileUseCase.getProfileById(accountId);
            return (profile != null && profile.getPlayerTag() != null)
                    ? profile.getPlayerTag().formatted()
                    : null;
        } catch (UserProfileNotFoundException e) {
            return null;
        }
    }
}
