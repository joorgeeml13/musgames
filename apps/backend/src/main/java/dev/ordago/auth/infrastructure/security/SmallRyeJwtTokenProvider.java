package dev.ordago.auth.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import io.smallrye.jwt.build.Jwt;
import io.smallrye.jwt.build.JwtClaimsBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import dev.ordago.auth.domain.model.Account;
import dev.ordago.auth.domain.model.UserSession;
import dev.ordago.auth.domain.port.out.TokenProviderPort;

@ApplicationScoped
public class SmallRyeJwtTokenProvider implements TokenProviderPort {

    private final String issuer;
    private final long accessTokenLifespanSeconds;
    private final Duration refreshTokenLifespan;
    private final SecureRandom secureRandom;

    public SmallRyeJwtTokenProvider(
            @ConfigProperty(name = "mp.jwt.verify.issuer", defaultValue = "https://mus.org/issuer") String issuer,
            @ConfigProperty(name = "ordago.auth.jwt.access-token-lifespan-seconds", defaultValue = "900") long accessTokenLifespanSeconds,
            @ConfigProperty(name = "ordago.auth.jwt.refresh-token-lifespan-days", defaultValue = "30") long refreshTokenLifespanDays
    ) {
        this.issuer = issuer;
        this.accessTokenLifespanSeconds = accessTokenLifespanSeconds;
        this.refreshTokenLifespan = Duration.ofDays(refreshTokenLifespanDays);
        this.secureRandom = new SecureRandom();
    }

    @Override
    public String createAccessToken(Account account, UserSession session, String playerTag) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(accessTokenLifespanSeconds);

        JwtClaimsBuilder builder = Jwt.issuer(issuer)
                .subject(account.getId().toString())
                .claim("sessionId", session.getId().toString())
                .claim("deviceId", session.getDeviceId())
                .claim("jti", UUID.randomUUID().toString())
                .issuedAt(now)
                .expiresAt(expiresAt);

        if (account.isGuest()) {
            Set<String> guestRoles = Set.of("GUEST");
            builder.upn("guest-" + account.getId())
                    .groups(guestRoles)
                    .claim("isGuest", true)
                    .claim("emailVerified", false)
                    .claim("roles", List.of("GUEST"))
                    .claim("isAdmin", false);
        } else {
            Set<String> roles = account.isSystemAdmin() ? Set.of("USER", "ADMIN") : Set.of("USER");
            builder.upn(account.getEmailValue() != null ? account.getEmailValue() : account.getId().toString())
                    .groups(roles)
                    .claim("isGuest", false)
                    .claim("roles", roles)
                    .claim("isAdmin", account.isSystemAdmin())
                    .claim("emailVerified", account.isEmailVerified());
            if (account.getEmailValue() != null) {
                builder.claim("email", account.getEmailValue());
            }
        }

        if (playerTag != null && !playerTag.isBlank()) {
            builder.claim("playerTag", playerTag);
        }

        return builder.sign();
    }

    @Override
    public String createAccessToken(Account account, UserSession session) {
        return createAccessToken(account, session, null);
    }

    @Override
    public String generateRefreshToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    @Override
    public String hashToken(String token) {
        if (token == null) {
            throw new IllegalArgumentException("Token cannot be null");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    @Override
    public long getAccessTokenLifespanSeconds() {
        return accessTokenLifespanSeconds;
    }

    @Override
    public Duration getRefreshTokenLifespan() {
        return refreshTokenLifespan;
    }
}
