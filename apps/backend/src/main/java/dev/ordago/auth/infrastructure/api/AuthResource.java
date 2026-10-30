package dev.ordago.auth.infrastructure.api;

import java.util.Objects;
import java.util.UUID;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import io.quarkus.security.Authenticated;
import io.vertx.core.http.HttpServerRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import dev.ordago.auth.domain.model.Account;
import dev.ordago.auth.domain.model.AuthTokens;
import dev.ordago.auth.domain.port.in.ChangePasswordUseCase;
import dev.ordago.auth.domain.port.in.GetCurrentAccountUseCase;
import dev.ordago.auth.domain.port.in.LoginUseCase;
import dev.ordago.auth.domain.port.in.LogoutUseCase;
import dev.ordago.auth.domain.port.in.RefreshTokenUseCase;
import dev.ordago.auth.domain.port.in.RegisterUseCase;
import dev.ordago.auth.domain.port.in.RegisterUseCase.AuthResult;
import dev.ordago.auth.domain.port.in.RequestPasswordResetUseCase;
import dev.ordago.auth.domain.port.in.ResendVerificationEmailUseCase;
import dev.ordago.auth.domain.port.in.ResetPasswordUseCase;
import dev.ordago.auth.domain.port.in.VerifyEmailUseCase;
import dev.ordago.auth.infrastructure.api.dto.AuthResponse;
import dev.ordago.auth.infrastructure.api.dto.ChangePasswordRequest;
import dev.ordago.auth.infrastructure.api.dto.ForgotPasswordRequest;
import dev.ordago.auth.infrastructure.api.dto.LoginRequest;
import dev.ordago.auth.infrastructure.api.dto.LogoutRequest;
import dev.ordago.auth.infrastructure.api.dto.RefreshTokenRequest;
import dev.ordago.auth.infrastructure.api.dto.RegisterRequest;
import dev.ordago.auth.infrastructure.api.dto.ResendVerificationRequest;
import dev.ordago.auth.infrastructure.api.dto.ResetPasswordRequest;
import dev.ordago.auth.infrastructure.api.dto.TokenResponse;
import dev.ordago.auth.infrastructure.api.dto.UserInfoResponse;
import dev.ordago.auth.infrastructure.api.dto.VerifyEmailRequest;
import dev.ordago.profile.domain.model.UserProfile;
import dev.ordago.profile.domain.port.in.GetUserProfileUseCase;
import dev.ordago.shared.infrastructure.api.response.ApiError;
import dev.ordago.shared.infrastructure.i18n.RequestLanguageContext;

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
    private final GetCurrentAccountUseCase getCurrentAccountUseCase;
    private final GetUserProfileUseCase getUserProfileUseCase;
    private final VerifyEmailUseCase verifyEmailUseCase;
    private final ResendVerificationEmailUseCase resendVerificationEmailUseCase;
    private final RequestPasswordResetUseCase requestPasswordResetUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;
    private final ChangePasswordUseCase changePasswordUseCase;
    private final RequestLanguageContext languageContext;
    private final JsonWebToken jwt;

    public AuthResource(
            RegisterUseCase registerUseCase,
            LoginUseCase loginUseCase,
            RefreshTokenUseCase refreshTokenUseCase,
            LogoutUseCase logoutUseCase,
            GetCurrentAccountUseCase getCurrentAccountUseCase,
            GetUserProfileUseCase getUserProfileUseCase,
            VerifyEmailUseCase verifyEmailUseCase,
            ResendVerificationEmailUseCase resendVerificationEmailUseCase,
            RequestPasswordResetUseCase requestPasswordResetUseCase,
            ResetPasswordUseCase resetPasswordUseCase,
            ChangePasswordUseCase changePasswordUseCase,
            RequestLanguageContext languageContext,
            JsonWebToken jwt
    ) {
        this.registerUseCase = Objects.requireNonNull(registerUseCase, "RegisterUseCase cannot be null");
        this.loginUseCase = Objects.requireNonNull(loginUseCase, "LoginUseCase cannot be null");
        this.refreshTokenUseCase = Objects.requireNonNull(refreshTokenUseCase, "RefreshTokenUseCase cannot be null");
        this.logoutUseCase = Objects.requireNonNull(logoutUseCase, "LogoutUseCase cannot be null");
        this.getCurrentAccountUseCase = Objects.requireNonNull(getCurrentAccountUseCase, "GetCurrentAccountUseCase cannot be null");
        this.getUserProfileUseCase = Objects.requireNonNull(getUserProfileUseCase, "GetUserProfileUseCase cannot be null");
        this.verifyEmailUseCase = Objects.requireNonNull(verifyEmailUseCase, "VerifyEmailUseCase cannot be null");
        this.resendVerificationEmailUseCase = Objects.requireNonNull(resendVerificationEmailUseCase, "ResendVerificationEmailUseCase cannot be null");
        this.requestPasswordResetUseCase = Objects.requireNonNull(requestPasswordResetUseCase, "RequestPasswordResetUseCase cannot be null");
        this.resetPasswordUseCase = Objects.requireNonNull(resetPasswordUseCase, "ResetPasswordUseCase cannot be null");
        this.changePasswordUseCase = Objects.requireNonNull(changePasswordUseCase, "ChangePasswordUseCase cannot be null");
        this.languageContext = Objects.requireNonNull(languageContext, "RequestLanguageContext cannot be null");
        this.jwt = Objects.requireNonNull(jwt, "JsonWebToken cannot be null");
    }

    @POST
    @Path("/register")
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
        return Response.status(Response.Status.CREATED)
                .entity(AuthResponse.of(result, profile))
                .build();
    }

    @POST
    @Path("/login")
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
        return Response.ok(AuthResponse.of(result, profile)).build();
    }

    @POST
    @Path("/refresh")
    @Operation(summary = "Refresh access token", description = "Rotates refresh token and issues a new JWT access token")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Token refreshed successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TokenResponse.class))),
            @APIResponse(responseCode = "400", description = "Validation failed or refresh token missing",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class))),
            @APIResponse(responseCode = "401", description = "Invalid or expired refresh token",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class)))
    })
    public Response refresh(@Valid RefreshTokenRequest request, @Context HttpServerRequest httpRequest) {
        String clientIp = extractClientIp(httpRequest);
        RefreshTokenUseCase.RefreshTokenCommand command = new RefreshTokenUseCase.RefreshTokenCommand(
                request.refreshToken(),
                clientIp
        );

        AuthTokens tokens = refreshTokenUseCase.refreshToken(command);
        return Response.ok(TokenResponse.of(tokens)).build();
    }

    @POST
    @Path("/logout")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.WILDCARD})
    @Operation(summary = "Logout user", description = "Invalidates the active session or specified refresh token")
    @APIResponses(value = {
            @APIResponse(responseCode = "204", description = "Session invalidated successfully")
    })
    public Response logout(LogoutRequest request) {
        UUID sessionId = extractOptionalSessionId();
        String refreshToken = request != null ? request.refreshToken() : null;

        logoutUseCase.logout(new LogoutUseCase.LogoutCommand(refreshToken, sessionId));
        return Response.noContent().build();
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
        return Response.noContent().build();
    }

    private String extractClientIp(HttpServerRequest httpRequest) {
        if (httpRequest == null) {
            return "127.0.0.1";
        }
        String ip = null;
        String forwarded = httpRequest.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            ip = forwarded.split(",")[0].trim();
        } else if (httpRequest.remoteAddress() != null) {
            ip = httpRequest.remoteAddress().host();
        }
        if (ip == null || ip.isBlank()) {
            return "127.0.0.1";
        }
        return ip.length() > 45 ? ip.substring(0, 45) : ip;
    }

    private UUID extractAccountId() {
        String subject = jwt.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new IllegalStateException("Authenticated user subject claim is missing");
        }
        return UUID.fromString(subject);
    }

    private UUID extractOptionalSessionId() {
        try {
            Object claim = jwt.getClaim("sessionId");
            if (claim != null) {
                return UUID.fromString(claim.toString());
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
