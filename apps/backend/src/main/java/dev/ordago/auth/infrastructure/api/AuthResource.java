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
import dev.ordago.auth.domain.port.in.GetCurrentAccountUseCase;
import dev.ordago.auth.domain.port.in.LoginUseCase;
import dev.ordago.auth.domain.port.in.LogoutUseCase;
import dev.ordago.auth.domain.port.in.RefreshTokenUseCase;
import dev.ordago.auth.domain.port.in.RegisterUseCase;
import dev.ordago.auth.domain.port.in.RegisterUseCase.AuthResult;
import dev.ordago.auth.infrastructure.api.dto.AuthResponse;
import dev.ordago.auth.infrastructure.api.dto.LoginRequest;
import dev.ordago.auth.infrastructure.api.dto.LogoutRequest;
import dev.ordago.auth.infrastructure.api.dto.RefreshTokenRequest;
import dev.ordago.auth.infrastructure.api.dto.RegisterRequest;
import dev.ordago.auth.infrastructure.api.dto.TokenResponse;
import dev.ordago.auth.infrastructure.api.dto.UserInfoResponse;
import dev.ordago.profile.domain.model.UserProfile;
import dev.ordago.profile.domain.port.in.GetUserProfileUseCase;
import dev.ordago.shared.infrastructure.api.response.ApiError;

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
    private final JsonWebToken jwt;

    public AuthResource(
            RegisterUseCase registerUseCase,
            LoginUseCase loginUseCase,
            RefreshTokenUseCase refreshTokenUseCase,
            LogoutUseCase logoutUseCase,
            GetCurrentAccountUseCase getCurrentAccountUseCase,
            GetUserProfileUseCase getUserProfileUseCase,
            JsonWebToken jwt
    ) {
        this.registerUseCase = Objects.requireNonNull(registerUseCase, "RegisterUseCase cannot be null");
        this.loginUseCase = Objects.requireNonNull(loginUseCase, "LoginUseCase cannot be null");
        this.refreshTokenUseCase = Objects.requireNonNull(refreshTokenUseCase, "RefreshTokenUseCase cannot be null");
        this.logoutUseCase = Objects.requireNonNull(logoutUseCase, "LogoutUseCase cannot be null");
        this.getCurrentAccountUseCase = Objects.requireNonNull(getCurrentAccountUseCase, "GetCurrentAccountUseCase cannot be null");
        this.getUserProfileUseCase = Objects.requireNonNull(getUserProfileUseCase, "GetUserProfileUseCase cannot be null");
        this.jwt = Objects.requireNonNull(jwt, "JsonWebToken cannot be null");
    }

    @POST
    @Path("/register")
    @Operation(summary = "Register a new player account", description = "Creates a new player account and initializes their profile with a Riot ID")
    @APIResponses(value = {
            @APIResponse(responseCode = "201", description = "Account created successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AuthResponse.class))),
            @APIResponse(responseCode = "400", description = "Validation failed for request fields",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class))),
            @APIResponse(responseCode = "409", description = "Email or Riot ID already registered",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class)))
    })
    public Response register(@Valid RegisterRequest request, @Context HttpServerRequest httpRequest) {
        String clientIp = extractClientIp(httpRequest);
        RegisterUseCase.RegisterCommand command = new RegisterUseCase.RegisterCommand(
                request.email(),
                request.password(),
                request.gameName(),
                request.tagLine(),
                request.deviceId(),
                request.deviceName(),
                clientIp
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
