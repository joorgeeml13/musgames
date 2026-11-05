package dev.ordago.auth.infrastructure.api;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.security.Authenticated;
import io.vertx.core.http.HttpServerRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import dev.ordago.auth.domain.model.Account;
import dev.ordago.auth.domain.model.AuthTokens;
import dev.ordago.auth.domain.model.OAuthProvider;
import dev.ordago.auth.domain.port.in.ChangePasswordUseCase;
import dev.ordago.auth.domain.port.in.ClaimGuestAccountUseCase;
import dev.ordago.auth.domain.port.in.ClaimGuestWithSocialUseCase;
import dev.ordago.auth.domain.port.in.ClaimGuestWithSocialUseCase.ClaimGuestSocialCommand;
import dev.ordago.auth.domain.port.in.GetCurrentAccountUseCase;
import dev.ordago.auth.domain.port.in.GetUserSessionsUseCase;
import dev.ordago.auth.domain.port.in.GuestLoginUseCase;
import dev.ordago.auth.domain.port.in.LoginUseCase;
import dev.ordago.auth.domain.port.in.LogoutUseCase;
import dev.ordago.auth.domain.port.in.RefreshTokenUseCase;
import dev.ordago.auth.domain.port.in.RegisterUseCase;
import dev.ordago.auth.domain.port.in.RegisterUseCase.AuthResult;
import dev.ordago.auth.domain.port.in.RequestPasswordResetUseCase;
import dev.ordago.auth.domain.port.in.ResendVerificationEmailUseCase;
import dev.ordago.auth.domain.port.in.ResetPasswordUseCase;
import dev.ordago.auth.domain.port.in.RevokeSessionUseCase;
import dev.ordago.auth.domain.port.in.SocialLoginUseCase;
import dev.ordago.auth.domain.port.in.SocialLoginUseCase.SocialLoginCommand;
import dev.ordago.auth.domain.port.in.VerifyEmailUseCase;
import dev.ordago.auth.infrastructure.api.dto.AuthResponse;
import dev.ordago.auth.infrastructure.api.dto.ChangePasswordRequest;
import dev.ordago.auth.infrastructure.api.dto.ClaimGuestRequest;
import dev.ordago.auth.infrastructure.api.dto.ClaimGuestSocialRequest;
import dev.ordago.auth.infrastructure.api.dto.ForgotPasswordRequest;
import dev.ordago.auth.infrastructure.api.dto.GuestLoginRequest;
import dev.ordago.auth.infrastructure.api.dto.LoginRequest;
import dev.ordago.auth.infrastructure.api.dto.LogoutRequest;
import dev.ordago.auth.infrastructure.api.dto.RefreshTokenRequest;
import dev.ordago.auth.infrastructure.api.dto.SocialLoginRequest;
import dev.ordago.auth.infrastructure.api.dto.RegisterRequest;
import dev.ordago.auth.infrastructure.api.dto.ResendVerificationRequest;
import dev.ordago.auth.infrastructure.api.dto.ResetPasswordRequest;
import dev.ordago.auth.infrastructure.api.dto.TokenResponse;
import dev.ordago.auth.infrastructure.api.dto.UserInfoResponse;
import dev.ordago.auth.infrastructure.api.dto.UserSessionResponse;
import dev.ordago.auth.infrastructure.api.dto.VerifyEmailRequest;
import dev.ordago.auth.infrastructure.security.AuthCookieService;
import dev.ordago.profile.domain.model.UserProfile;
import dev.ordago.profile.domain.port.in.GetUserProfileUseCase;
import dev.ordago.shared.domain.exception.ValidationException;
import dev.ordago.shared.infrastructure.api.response.ApiError;
import dev.ordago.shared.infrastructure.i18n.RequestLanguageContext;
import dev.ordago.shared.infrastructure.ratelimit.ClientIpResolver;
import dev.ordago.shared.infrastructure.ratelimit.RateLimited;
import dev.ordago.shared.infrastructure.ratelimit.RateLimitTier;

@Path("/api/v1/auth")
@Tag(name = "Authentication", description = "Operations for registration, login, token refresh and session lifecycle")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class AuthResource {

    private final RegisterUseCase registerUseCase;
    private final LoginUseCase loginUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogoutUseCase logoutUseCase;
    private final GetUserSessionsUseCase getUserSessionsUseCase;
    private final RevokeSessionUseCase revokeSessionUseCase;
    private final GetCurrentAccountUseCase getCurrentAccountUseCase;
    private final GetUserProfileUseCase getUserProfileUseCase;
    private final VerifyEmailUseCase verifyEmailUseCase;
    private final ResendVerificationEmailUseCase resendVerificationEmailUseCase;
    private final RequestPasswordResetUseCase requestPasswordResetUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;
    private final ChangePasswordUseCase changePasswordUseCase;
    private final GuestLoginUseCase guestLoginUseCase;
    private final ClaimGuestAccountUseCase claimGuestAccountUseCase;
    private final SocialLoginUseCase socialLoginUseCase;
    private final ClaimGuestWithSocialUseCase claimGuestWithSocialUseCase;
    private final AuthCookieService authCookieService;
    private final RequestLanguageContext languageContext;
    private final JsonWebToken jwt;
    private final ObjectMapper objectMapper;
    private final ClientIpResolver clientIpResolver;

    @Inject
    public AuthResource(
            RegisterUseCase registerUseCase,
            LoginUseCase loginUseCase,
            RefreshTokenUseCase refreshTokenUseCase,
            LogoutUseCase logoutUseCase,
            GetUserSessionsUseCase getUserSessionsUseCase,
            RevokeSessionUseCase revokeSessionUseCase,
            GetCurrentAccountUseCase getCurrentAccountUseCase,
            GetUserProfileUseCase getUserProfileUseCase,
            VerifyEmailUseCase verifyEmailUseCase,
            ResendVerificationEmailUseCase resendVerificationEmailUseCase,
            RequestPasswordResetUseCase requestPasswordResetUseCase,
            ResetPasswordUseCase resetPasswordUseCase,
            ChangePasswordUseCase changePasswordUseCase,
            GuestLoginUseCase guestLoginUseCase,
            ClaimGuestAccountUseCase claimGuestAccountUseCase,
            SocialLoginUseCase socialLoginUseCase,
            ClaimGuestWithSocialUseCase claimGuestWithSocialUseCase,
            AuthCookieService authCookieService,
            RequestLanguageContext languageContext,
            JsonWebToken jwt,
            ObjectMapper objectMapper,
            ClientIpResolver clientIpResolver
    ) {
        this.registerUseCase = Objects.requireNonNull(registerUseCase, "RegisterUseCase cannot be null");
        this.loginUseCase = Objects.requireNonNull(loginUseCase, "LoginUseCase cannot be null");
        this.refreshTokenUseCase = Objects.requireNonNull(refreshTokenUseCase, "RefreshTokenUseCase cannot be null");
        this.logoutUseCase = Objects.requireNonNull(logoutUseCase, "LogoutUseCase cannot be null");
        this.getUserSessionsUseCase = Objects.requireNonNull(getUserSessionsUseCase, "GetUserSessionsUseCase cannot be null");
        this.revokeSessionUseCase = Objects.requireNonNull(revokeSessionUseCase, "RevokeSessionUseCase cannot be null");
        this.getCurrentAccountUseCase = Objects.requireNonNull(getCurrentAccountUseCase, "GetCurrentAccountUseCase cannot be null");
        this.getUserProfileUseCase = Objects.requireNonNull(getUserProfileUseCase, "GetUserProfileUseCase cannot be null");
        this.verifyEmailUseCase = Objects.requireNonNull(verifyEmailUseCase, "VerifyEmailUseCase cannot be null");
        this.resendVerificationEmailUseCase = Objects.requireNonNull(resendVerificationEmailUseCase, "ResendVerificationEmailUseCase cannot be null");
        this.requestPasswordResetUseCase = Objects.requireNonNull(requestPasswordResetUseCase, "RequestPasswordResetUseCase cannot be null");
        this.resetPasswordUseCase = Objects.requireNonNull(resetPasswordUseCase, "ResetPasswordUseCase cannot be null");
        this.changePasswordUseCase = Objects.requireNonNull(changePasswordUseCase, "ChangePasswordUseCase cannot be null");
        this.guestLoginUseCase = Objects.requireNonNull(guestLoginUseCase, "GuestLoginUseCase cannot be null");
        this.claimGuestAccountUseCase = Objects.requireNonNull(claimGuestAccountUseCase, "ClaimGuestAccountUseCase cannot be null");
        this.socialLoginUseCase = Objects.requireNonNull(socialLoginUseCase, "SocialLoginUseCase cannot be null");
        this.claimGuestWithSocialUseCase = Objects.requireNonNull(claimGuestWithSocialUseCase, "ClaimGuestWithSocialUseCase cannot be null");
        this.authCookieService = Objects.requireNonNull(authCookieService, "AuthCookieService cannot be null");
        this.languageContext = Objects.requireNonNull(languageContext, "RequestLanguageContext cannot be null");
        this.jwt = Objects.requireNonNull(jwt, "JsonWebToken cannot be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "ObjectMapper cannot be null");
        this.clientIpResolver = Objects.requireNonNull(clientIpResolver, "ClientIpResolver cannot be null");
    }

    @POST
    @Path("/register")
    @RateLimited(RateLimitTier.REGISTER)
    @Operation(summary = "Register a new player account", description = "Creates a new player account and initializes their profile with a player tag")
    @APIResponses(value = {
            @APIResponse(responseCode = "201", description = "Account created successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AuthResponse.class))),
            @APIResponse(responseCode = "400", description = "Validation failed for request fields",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class))),
            @APIResponse(responseCode = "409", description = "Email or player tag already registered",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class)))
    })
    public Response register(@Valid RegisterRequest request, @Context HttpServerRequest httpRequest) {
        String clientIp = extractClientIp(httpRequest);
        String language = (languageContext != null && languageContext.getLanguage() != null)
                ? languageContext.getLanguage().getCode()
                : "es";
        RegisterUseCase.RegisterCommand command = new RegisterUseCase.RegisterCommand(
                request.email(),
                request.password(),
                request.gameName(),
                request.tagLine(),
                request.deviceId(),
                request.deviceName(),
                clientIp,
                language
        );

        AuthResult result = registerUseCase.register(command);
        UserProfile profile = getUserProfileUseCase.getProfileById(result.account().getId());
        NewCookie accessCookie = authCookieService.createAccessTokenCookie(
                result.tokens().accessToken(),
                result.tokens().expiresInSeconds()
        );
        NewCookie refreshCookie = authCookieService.createRefreshTokenCookie(
                result.tokens().refreshToken()
        );
        return Response.status(Response.Status.CREATED)
                .cookie(accessCookie, refreshCookie)
                .entity(AuthResponse.of(result, profile))
                .build();
    }

    @POST
    @Path("/guest")
    @RateLimited(RateLimitTier.REGISTER)
    @Operation(summary = "Authenticate anonymously as guest", description = "Creates a temporary guest account and returns access and refresh tokens")
    @APIResponses(value = {
            @APIResponse(responseCode = "201", description = "Guest account created successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AuthResponse.class))),
            @APIResponse(responseCode = "400", description = "Validation failed for request fields",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class)))
    })
    public Response guestLogin(@Valid GuestLoginRequest request, @Context HttpServerRequest httpRequest) {
        String clientIp = extractClientIp(httpRequest);
        String deviceId = request != null ? request.deviceId() : null;
        String deviceName = request != null ? request.deviceName() : null;
        GuestLoginUseCase.GuestLoginCommand command = new GuestLoginUseCase.GuestLoginCommand(
                deviceId,
                deviceName,
                clientIp
        );

        AuthResult result = guestLoginUseCase.loginAsGuest(command);
        UserProfile profile = getUserProfileUseCase.getProfileById(result.account().getId());
        NewCookie accessCookie = authCookieService.createAccessTokenCookie(
                result.tokens().accessToken(),
                result.tokens().expiresInSeconds()
        );
        NewCookie refreshCookie = authCookieService.createRefreshTokenCookie(
                result.tokens().refreshToken()
        );
        return Response.status(Response.Status.CREATED)
                .cookie(accessCookie, refreshCookie)
                .entity(AuthResponse.of(result, profile))
                .build();
    }

    @POST
    @Path("/guest/claim")
    @Authenticated
    @RateLimited(RateLimitTier.LOGIN)
    @SecurityRequirement(name = "jwtAuth")
    @Operation(summary = "Claim guest account into regular account", description = "Converts an existing anonymous guest account into a registered account with email and password")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Guest account claimed successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AuthResponse.class))),
            @APIResponse(responseCode = "400", description = "Validation failed or account is not a guest",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class))),
            @APIResponse(responseCode = "401", description = "Unauthorized or expired token",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class))),
            @APIResponse(responseCode = "409", description = "Email is already registered",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class)))
    })
    public Response claimGuest(@Valid ClaimGuestRequest request, @Context HttpServerRequest httpRequest) {
        UUID accountId = extractAccountId();
        String clientIp = extractClientIp(httpRequest);
        String language = (languageContext != null && languageContext.getLanguage() != null)
                ? languageContext.getLanguage().getCode()
                : "es";

        ClaimGuestAccountUseCase.ClaimGuestCommand command = new ClaimGuestAccountUseCase.ClaimGuestCommand(
                accountId,
                request.email(),
                request.password(),
                request.deviceId(),
                request.deviceName(),
                clientIp,
                language
        );

        AuthResult result = claimGuestAccountUseCase.claimGuestAccount(command);
        UserProfile profile = getUserProfileUseCase.getProfileById(result.account().getId());
        NewCookie accessCookie = authCookieService.createAccessTokenCookie(
                result.tokens().accessToken(),
                result.tokens().expiresInSeconds()
        );
        NewCookie refreshCookie = authCookieService.createRefreshTokenCookie(
                result.tokens().refreshToken()
        );
        return Response.ok(AuthResponse.of(result, profile))
                .cookie(accessCookie, refreshCookie)
                .build();
    }

    @POST
    @Path("/guest/claim-social/{provider}")
    @Authenticated
    @RateLimited(RateLimitTier.LOGIN)
    @SecurityRequirement(name = "jwtAuth")
    @Operation(summary = "Claim guest account with OAuth provider", description = "Converts an existing anonymous guest account into a permanent account linked with an OAuth provider")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Guest account claimed successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AuthResponse.class))),
            @APIResponse(responseCode = "400", description = "Validation failed, account is not guest, or unsupported provider",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class))),
            @APIResponse(responseCode = "401", description = "Unauthorized or invalid OAuth token",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class))),
            @APIResponse(responseCode = "409", description = "OAuth identity already linked or email already registered",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class)))
    })
    public Response claimGuestSocial(
            @PathParam("provider") String providerStr,
            @Valid ClaimGuestSocialRequest request,
            @Context HttpServerRequest httpRequest
    ) {
        UUID accountId = extractAccountId();
        OAuthProvider provider = OAuthProvider.fromString(providerStr);
        String clientIp = extractClientIp(httpRequest);

        ClaimGuestSocialCommand command = new ClaimGuestSocialCommand(
                accountId,
                provider,
                request.token(),
                request.deviceId(),
                request.deviceName(),
                clientIp
        );

        AuthResult result = claimGuestWithSocialUseCase.claimGuestWithSocial(command);
        UserProfile profile = getUserProfileUseCase.getProfileById(result.account().getId());
        NewCookie accessCookie = authCookieService.createAccessTokenCookie(
                result.tokens().accessToken(),
                result.tokens().expiresInSeconds()
        );
        NewCookie refreshCookie = authCookieService.createRefreshTokenCookie(
                result.tokens().refreshToken()
        );
        return Response.ok(AuthResponse.of(result, profile))
                .cookie(accessCookie, refreshCookie)
                .build();
    }

    @POST
    @Path("/oauth/{provider}")
    @RateLimited(RateLimitTier.LOGIN)
    @Operation(summary = "Authenticate user with OAuth provider", description = "Verifies OAuth token with identity provider, finds or creates account, records device session, and returns JWT tokens")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Authentication successful",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AuthResponse.class))),
            @APIResponse(responseCode = "400", description = "Validation failed or unsupported provider",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class))),
            @APIResponse(responseCode = "401", description = "Invalid OAuth token",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class)))
    })
    public Response oauthLogin(
            @PathParam("provider") String providerStr,
            @Valid SocialLoginRequest request,
            @Context HttpServerRequest httpRequest
    ) {
        OAuthProvider provider = OAuthProvider.fromString(providerStr);
        String clientIp = extractClientIp(httpRequest);
        SocialLoginCommand command = new SocialLoginCommand(
                provider,
                request.token(),
                request.deviceId(),
                request.deviceName(),
                clientIp
        );

        AuthResult result = socialLoginUseCase.socialLogin(command);
        UserProfile profile = getUserProfileUseCase.getProfileById(result.account().getId());
        NewCookie accessCookie = authCookieService.createAccessTokenCookie(
                result.tokens().accessToken(),
                result.tokens().expiresInSeconds()
        );
        NewCookie refreshCookie = authCookieService.createRefreshTokenCookie(
                result.tokens().refreshToken()
        );
        return Response.ok(AuthResponse.of(result, profile))
                .cookie(accessCookie, refreshCookie)
                .build();
    }

    @POST
    @Path("/login")
    @RateLimited(RateLimitTier.LOGIN)
    @Operation(summary = "Authenticate user", description = "Verifies credentials, records active device session, and returns JWT access and refresh tokens")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Authentication successful",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AuthResponse.class))),
            @APIResponse(responseCode = "400", description = "Validation failed for request fields",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class))),
            @APIResponse(responseCode = "401", description = "Invalid credentials",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class))),
            @APIResponse(responseCode = "423", description = "Account is temporarily locked due to repeated failed login attempts",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class)))
    })
    public Response login(@Valid LoginRequest request, @Context HttpServerRequest httpRequest) {
        String clientIp = extractClientIp(httpRequest);
        LoginUseCase.LoginCommand command = new LoginUseCase.LoginCommand(
                request.email(),
                request.password(),
                request.deviceId(),
                request.deviceName(),
                clientIp
        );

        AuthResult result = loginUseCase.login(command);
        UserProfile profile = getUserProfileUseCase.getProfileById(result.account().getId());
        NewCookie accessCookie = authCookieService.createAccessTokenCookie(
                result.tokens().accessToken(),
                result.tokens().expiresInSeconds()
        );
        NewCookie refreshCookie = authCookieService.createRefreshTokenCookie(
                result.tokens().refreshToken()
        );
        return Response.ok(AuthResponse.of(result, profile))
                .cookie(accessCookie, refreshCookie)
                .build();
    }

    @POST
    @Path("/refresh")
    @RateLimited(RateLimitTier.REFRESH)
    @Consumes({MediaType.APPLICATION_JSON, MediaType.WILDCARD})
    @Operation(summary = "Refresh access token", description = "Rotates refresh token and issues a new JWT access token")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Token refreshed successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TokenResponse.class))),
            @APIResponse(responseCode = "400", description = "Validation failed or refresh token missing",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class))),
            @APIResponse(responseCode = "401", description = "Invalid or expired refresh token",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class)))
    })
    public Response refresh(
            @CookieParam("refresh_token") Cookie refreshCookie,
            @Valid RefreshTokenRequest request,
            @Context HttpServerRequest httpRequest
    ) {
        String refreshToken = authCookieService.extractRefreshToken(
                refreshCookie,
                request != null ? request.refreshToken() : null
        );

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new ValidationException("Refresh token is required");
        }

        String clientIp = extractClientIp(httpRequest);
        RefreshTokenUseCase.RefreshTokenCommand command = new RefreshTokenUseCase.RefreshTokenCommand(
                refreshToken,
                clientIp
        );

        AuthTokens tokens = refreshTokenUseCase.refreshToken(command);
        NewCookie accessCookie = authCookieService.createAccessTokenCookie(
                tokens.accessToken(),
                tokens.expiresInSeconds()
        );
        NewCookie newRefreshCookie = authCookieService.createRefreshTokenCookie(
                tokens.refreshToken()
        );
        return Response.ok(TokenResponse.of(tokens))
                .cookie(accessCookie, newRefreshCookie)
                .build();
    }

    @POST
    @Path("/logout")
    @Authenticated
    @SecurityRequirement(name = "jwtAuth")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.WILDCARD})
    @Operation(summary = "Logout user", description = "Invalidates the active session associated with the JWT sessionId and clears authentication cookies")
    @APIResponses(value = {
            @APIResponse(responseCode = "204", description = "Session invalidated successfully"),
            @APIResponse(responseCode = "401", description = "Unauthorized or expired token",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class)))
    })
    public Response logout(
            @CookieParam("refresh_token") Cookie refreshCookie,
            @RequestBody(description = "Optional logout payload for mobile fallback", required = false,
                    content = @Content(schema = @Schema(implementation = LogoutRequest.class)))
            String rawBody
    ) {
        UUID sessionId = extractSessionId();
        String bodyRefreshToken = extractRefreshTokenFromBody(rawBody);
        String refreshToken = authCookieService.extractRefreshToken(
                refreshCookie,
                bodyRefreshToken
        );

        logoutUseCase.logout(new LogoutUseCase.LogoutCommand(refreshToken, sessionId));
        return Response.noContent()
                .cookie(authCookieService.clearAccessTokenCookie(), authCookieService.clearRefreshTokenCookie())
                .build();
    }

    private String extractRefreshTokenFromBody(String rawBody) {
        if (rawBody == null || rawBody.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(rawBody).path("refreshToken").asText(null);
        } catch (Exception ignored) {
            return null;
        }
    }

    @GET
    @Path("/sessions")
    @Authenticated
    @SecurityRequirement(name = "jwtAuth")
    @Operation(summary = "List active user sessions", description = "List all active device sessions for the authenticated user and identify the current device")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Active device sessions retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = UserSessionResponse[].class))),
            @APIResponse(responseCode = "401", description = "Unauthorized or expired token",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class)))
    })
    public Response getActiveSessions() {
        UUID accountId = extractAccountId();
        UUID currentSessionId = extractSessionId();
        List<UserSessionResponse> sessions = getUserSessionsUseCase.getActiveSessions(accountId)
                .stream()
                .map(session -> UserSessionResponse.of(session, currentSessionId))
                .toList();
        return Response.ok(sessions).build();
    }

    @DELETE
    @Path("/sessions/{id}")
    @Authenticated
    @SecurityRequirement(name = "jwtAuth")
    @Operation(summary = "Revoke specific user session", description = "Revokes a specific device session belonging to the authenticated user")
    @APIResponses(value = {
            @APIResponse(responseCode = "204", description = "Session revoked successfully"),
            @APIResponse(responseCode = "401", description = "Unauthorized or expired token",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class))),
            @APIResponse(responseCode = "404", description = "Session not found or belongs to another user",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class)))
    })
    public Response revokeSession(@PathParam("id") UUID sessionId) {
        UUID accountId = extractAccountId();
        UUID currentSessionId = extractSessionId();

        revokeSessionUseCase.revokeSession(accountId, sessionId, "USER_MANUAL_REVOCATION");

        Response.ResponseBuilder response = Response.noContent();
        if (sessionId.equals(currentSessionId)) {
            response.cookie(authCookieService.clearAccessTokenCookie(), authCookieService.clearRefreshTokenCookie());
        }
        return response.build();
    }

    @DELETE
    @Path("/sessions")
    @Authenticated
    @SecurityRequirement(name = "jwtAuth")
    @Operation(summary = "Revoke user sessions", description = "Revokes user device sessions with optional exclusion of current session")
    @APIResponses(value = {
            @APIResponse(responseCode = "204", description = "Sessions revoked successfully"),
            @APIResponse(responseCode = "401", description = "Unauthorized or expired token",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class)))
    })
    public Response revokeSessions(
            @QueryParam("exclude_current") @DefaultValue("false") boolean excludeCurrent
    ) {
        UUID accountId = extractAccountId();
        UUID currentSessionId = extractSessionId();

        if (excludeCurrent) {
            revokeSessionUseCase.revokeOtherSessions(accountId, currentSessionId, "USER_REVOKED_OTHER_DEVICES");
            return Response.noContent().build();
        } else {
            revokeSessionUseCase.revokeAllSessionsForAccount(accountId, "USER_REVOKED_ALL_DEVICES");
            return Response.noContent()
                    .cookie(authCookieService.clearAccessTokenCookie(), authCookieService.clearRefreshTokenCookie())
                    .build();
        }
    }

    @GET
    @Path("/me")
    @Authenticated
    @SecurityRequirement(name = "jwtAuth")
    @Operation(summary = "Get current authenticated user info", description = "Retrieves account and profile summary for the bearer token owner")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Current user information retrieved",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = UserInfoResponse.class))),
            @APIResponse(responseCode = "401", description = "Unauthorized or expired token",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class)))
    })
    public Response getCurrentUser() {
        UUID accountId = extractAccountId();
        Account account = getCurrentAccountUseCase.getAccountById(accountId);
        UserProfile profile = getUserProfileUseCase.getProfileById(accountId);
        return Response.ok(UserInfoResponse.of(account, profile)).build();
    }

    @POST
    @Path("/verify-email")
    @Operation(summary = "Verify account email", description = "Validates the email verification token and marks the account email as verified")
    @APIResponses(value = {
            @APIResponse(responseCode = "204", description = "Email verified successfully"),
            @APIResponse(responseCode = "400", description = "Invalid or expired verification token",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class)))
    })
    public Response verifyEmail(@Valid VerifyEmailRequest request) {
        verifyEmailUseCase.verifyEmail(request.token());
        return Response.noContent().build();
    }

    @POST
    @Path("/resend-verification")
    @RateLimited(RateLimitTier.RESEND_VERIFICATION)
    @Operation(summary = "Resend verification email", description = "Sends a new verification token to the user's email if the account exists and is not verified")
    @APIResponses(value = {
            @APIResponse(responseCode = "204", description = "Verification email sent if account exists and is unverified"),
            @APIResponse(responseCode = "400", description = "Validation failed for request fields",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class)))
    })
    public Response resendVerification(@Valid ResendVerificationRequest request) {
        String language = (languageContext != null && languageContext.getLanguage() != null)
                ? languageContext.getLanguage().getCode()
                : "es";
        resendVerificationEmailUseCase.resendVerificationEmail(request.email(), language);
        return Response.noContent().build();
    }

    @POST
    @Path("/forgot-password")
    @RateLimited(RateLimitTier.PASSWORD_RESET)
    @Operation(summary = "Request password reset", description = "Generates a password reset token and sends a reset email if the account exists and is active")
    @APIResponses(value = {
            @APIResponse(responseCode = "204", description = "Password reset instructions sent if account exists and is active"),
            @APIResponse(responseCode = "400", description = "Validation failed for request fields",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class)))
    })
    public Response forgotPassword(@Valid ForgotPasswordRequest request) {
        String language = (languageContext != null && languageContext.getLanguage() != null)
                ? languageContext.getLanguage().getCode()
                : "es";
        requestPasswordResetUseCase.requestPasswordReset(request.email(), language);
        return Response.noContent().build();
    }

    @POST
    @Path("/reset-password")
    @Operation(summary = "Reset password with token", description = "Resets user password using the token received by email and revokes all active sessions")
    @APIResponses(value = {
            @APIResponse(responseCode = "204", description = "Password reset successfully"),
            @APIResponse(responseCode = "400", description = "Invalid or expired reset token or validation failed",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class)))
    })
    public Response resetPassword(@Valid ResetPasswordRequest request) {
        resetPasswordUseCase.resetPassword(request.token(), request.newPassword());
        return Response.noContent().build();
    }

    @POST
    @Path("/change-password")
    @Authenticated
    @SecurityRequirement(name = "jwtAuth")
    @Operation(summary = "Change password for authenticated user", description = "Verifies current password, updates it to the new password, and revokes all active sessions")
    @APIResponses(value = {
            @APIResponse(responseCode = "204", description = "Password changed successfully"),
            @APIResponse(responseCode = "400", description = "Validation failed for request fields",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class))),
            @APIResponse(responseCode = "401", description = "Invalid current credentials or unauthorized",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class)))
    })
    public Response changePassword(@Valid ChangePasswordRequest request) {
        UUID accountId = extractAccountId();
        changePasswordUseCase.changePassword(accountId, request.currentPassword(), request.newPassword());
        return Response.noContent()
                .cookie(authCookieService.clearAccessTokenCookie(), authCookieService.clearRefreshTokenCookie())
                .build();
    }

    private String extractClientIp(HttpServerRequest httpRequest) {
        return clientIpResolver.resolve(httpRequest);
    }

    private UUID extractAccountId() {
        String subject = jwt.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new IllegalStateException("Authenticated user subject claim is missing");
        }
        return UUID.fromString(subject);
    }

    private UUID extractSessionId() {
        Object claim = jwt.getClaim("sessionId");
        if (claim == null || claim.toString().isBlank()) {
            throw new IllegalStateException("Authenticated user sessionId claim is missing");
        }
        return UUID.fromString(claim.toString());
    }
}
