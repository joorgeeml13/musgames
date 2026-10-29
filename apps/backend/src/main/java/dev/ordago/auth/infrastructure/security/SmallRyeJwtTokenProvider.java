package dev.ordago.auth.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Set;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import io.smallrye.jwt.build.Jwt;
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
    public String createAccessToken(Account account, UserSession session) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(accessTokenLifespanSeconds);
        Set<String> roles = account.isSystemAdmin() ? Set.of("USER", "ADMIN") : Set.of("USER");

        return Jwt.issuer(issuer)
                .subject(account.getId().toString())
                .upn(account.getEmail().value())
                .groups(roles)
                .claim("sessionId", session.getId().toString())
                .claim("deviceId", session.getDeviceId())
                .claim("email", account.getEmail().value())
                .claim("isAdmin", account.isSystemAdmin())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .sign();
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
