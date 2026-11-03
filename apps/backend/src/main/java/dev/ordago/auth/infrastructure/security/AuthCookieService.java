package dev.ordago.auth.infrastructure.security;

import java.time.Duration;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.NewCookie;

@ApplicationScoped
public class AuthCookieService {

    public static final String ACCESS_TOKEN_COOKIE_NAME = "access_token";
    public static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";
    public static final String ACCESS_COOKIE_PATH = "/";
    public static final String REFRESH_COOKIE_PATH = "/api/v1/auth";

    private final boolean cookieSecure;
    private final NewCookie.SameSite sameSite;
    private final Duration defaultRefreshTokenDuration;

    public AuthCookieService(
            @ConfigProperty(name = "ordago.auth.cookie.secure", defaultValue = "false") boolean cookieSecure,
            @ConfigProperty(name = "ordago.auth.cookie.same-site", defaultValue = "STRICT") String sameSiteConfig,
            @ConfigProperty(name = "ordago.auth.jwt.refresh-token-lifespan-days", defaultValue = "30") long refreshTokenLifespanDays
    ) {
        this.cookieSecure = cookieSecure;
        this.sameSite = parseSameSite(sameSiteConfig);
        this.defaultRefreshTokenDuration = Duration.ofDays(refreshTokenLifespanDays);
    }

    public NewCookie createAccessTokenCookie(String accessToken, long expiresInSeconds) {
        return new NewCookie.Builder(ACCESS_TOKEN_COOKIE_NAME)
                .value(accessToken)
                .path(ACCESS_COOKIE_PATH)
                .maxAge((int) expiresInSeconds)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(sameSite)
                .build();
    }

    public NewCookie createRefreshTokenCookie(String refreshToken, Duration duration) {
        return new NewCookie.Builder(REFRESH_TOKEN_COOKIE_NAME)
                .value(refreshToken)
                .path(REFRESH_COOKIE_PATH)
                .maxAge((int) duration.toSeconds())
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(sameSite)
                .build();
    }

    public NewCookie createRefreshTokenCookie(String refreshToken) {
        return createRefreshTokenCookie(refreshToken, defaultRefreshTokenDuration);
    }

    public NewCookie clearAccessTokenCookie() {
        return new NewCookie.Builder(ACCESS_TOKEN_COOKIE_NAME)
                .value("")
                .path(ACCESS_COOKIE_PATH)
                .maxAge(0)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(sameSite)
                .build();
    }

    public NewCookie clearRefreshTokenCookie() {
        return new NewCookie.Builder(REFRESH_TOKEN_COOKIE_NAME)
                .value("")
                .path(REFRESH_COOKIE_PATH)
                .maxAge(0)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(sameSite)
                .build();
    }

    public String extractRefreshToken(Cookie cookieParam, String bodyToken) {
        if (cookieParam != null && cookieParam.getValue() != null && !cookieParam.getValue().isBlank()) {
            return cookieParam.getValue().trim();
        }
        if (bodyToken != null && !bodyToken.isBlank()) {
            return bodyToken.trim();
        }
        return null;
    }

    public Duration getDefaultRefreshTokenDuration() {
        return defaultRefreshTokenDuration;
    }

    private static NewCookie.SameSite parseSameSite(String sameSiteConfig) {
        if (sameSiteConfig == null || sameSiteConfig.isBlank()) {
            return NewCookie.SameSite.STRICT;
        }
        try {
            return NewCookie.SameSite.valueOf(sameSiteConfig.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return NewCookie.SameSite.STRICT;
        }
    }
}
