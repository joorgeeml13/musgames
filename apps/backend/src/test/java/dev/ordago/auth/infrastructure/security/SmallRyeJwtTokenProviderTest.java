package dev.ordago.auth.infrastructure.security;

import java.time.Duration;
import java.util.Base64;
import java.util.UUID;
import jakarta.inject.Inject;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.jwt.auth.principal.DefaultJWTParser;
import dev.ordago.auth.domain.model.Account;
import dev.ordago.auth.domain.model.UserSession;
import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class SmallRyeJwtTokenProviderTest {

    @Inject
    SmallRyeJwtTokenProvider tokenProvider;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DefaultJWTParser jwtParser = new DefaultJWTParser();

    @Test
    @DisplayName("Should create access token with playerTag and emailVerified claims when verified")
    void shouldCreateAccessTokenWithPlayerTagAndEmailVerifiedTrue() throws Exception {
        UUID accountId = UUID.randomUUID();
        Account account = Account.createNew(accountId, "itmusmaster@ordago.dev", "hashedPassword");
        account.verifyEmail(account.getEmailVerificationToken());

        UserSession session = UserSession.createNew(
                UUID.randomUUID(),
                accountId,
                "device-xyz",
                "Firefox Linux",
                "refresh-hash",
                Duration.ofDays(30)
        );

        String token = tokenProvider.createAccessToken(account, session, "ITMusMaster#ESP");

        assertThat(token).isNotBlank();

        // 1. Verify via Base64 JSON decoding
        JsonNode payloadJson = decodeJwtPayload(token);
        assertThat(payloadJson.get("iss").asText()).isEqualTo("https://mus.org/issuer");
        assertThat(payloadJson.get("sub").asText()).isEqualTo(accountId.toString());
        assertThat(payloadJson.get("upn").asText()).isEqualTo("itmusmaster@ordago.dev");
        assertThat(payloadJson.get("email").asText()).isEqualTo("itmusmaster@ordago.dev");
        assertThat(payloadJson.get("sessionId").asText()).isEqualTo(session.getId().toString());
        assertThat(payloadJson.get("deviceId").asText()).isEqualTo("device-xyz");
        assertThat(payloadJson.get("isAdmin").asBoolean()).isFalse();
        assertThat(payloadJson.get("emailVerified").isBoolean()).isTrue();
        assertThat(payloadJson.get("emailVerified").asBoolean()).isTrue();
        assertThat(payloadJson.get("playerTag").isTextual()).isTrue();
        assertThat(payloadJson.get("playerTag").asText()).isEqualTo("ITMusMaster#ESP");
        assertThat(payloadJson.get("jti").asText()).isNotBlank();
        UUID.fromString(payloadJson.get("jti").asText()); // validates UUID format

        // 2. Verify via DefaultJWTParser
        JsonWebToken jwt = jwtParser.parseOnly(token);
        assertThat(jwt.getSubject()).isEqualTo(accountId.toString());
        assertThat(jwt.getName()).isEqualTo("itmusmaster@ordago.dev");
        assertThat(jwt.getClaim("emailVerified").toString()).isEqualTo("true");
        assertThat(jwt.getClaim("playerTag").toString().replace("\"", "")).isEqualTo("ITMusMaster#ESP");
        assertThat(jwt.getTokenID()).isEqualTo(payloadJson.get("jti").asText());
    }

    @Test
    @DisplayName("Should create access token with emailVerified false for unverified account")
    void shouldCreateAccessTokenWithEmailVerifiedFalse() throws Exception {
        UUID accountId = UUID.randomUUID();
        Account account = Account.createNew(accountId, "unverified@ordago.dev", "hashedPassword");

        UserSession session = UserSession.createNew(
                UUID.randomUUID(),
                accountId,
                "device-abc",
                "Chrome Mobile",
                "refresh-hash",
                Duration.ofDays(30)
        );

        String token = tokenProvider.createAccessToken(account, session, "Unverified#1234");

        JsonNode payloadJson = decodeJwtPayload(token);
        assertThat(payloadJson.get("emailVerified").isBoolean()).isTrue();
        assertThat(payloadJson.get("emailVerified").asBoolean()).isFalse();
        assertThat(payloadJson.get("playerTag").isTextual()).isTrue();
        assertThat(payloadJson.get("playerTag").asText()).isEqualTo("Unverified#1234");

        JsonWebToken jwt = jwtParser.parseOnly(token);
        assertThat(jwt.getClaim("emailVerified").toString()).isEqualTo("false");
        assertThat(jwt.getClaim("playerTag").toString().replace("\"", "")).isEqualTo("Unverified#1234");
    }

    @Test
    @DisplayName("Should omit playerTag claim when playerTag is null")
    void shouldOmitPlayerTagClaimWhenNull() throws Exception {
        UUID accountId = UUID.randomUUID();
        Account account = Account.createNew(accountId, "notag@ordago.dev", "hashedPassword");

        UserSession session = UserSession.createNew(
                UUID.randomUUID(),
                accountId,
                "device-1",
                "Chrome",
                "refresh-hash",
                Duration.ofDays(30)
        );

        String token = tokenProvider.createAccessToken(account, session, null);

        JsonNode payloadJson = decodeJwtPayload(token);
        assertThat(payloadJson.has("playerTag")).isFalse();
        assertThat(payloadJson.get("emailVerified").asBoolean()).isFalse();
        assertThat(payloadJson.get("jti").asText()).isNotBlank();

        JsonWebToken jwt = jwtParser.parseOnly(token);
        assertThat(jwt.containsClaim("playerTag")).isFalse();
    }

    @Test
    @DisplayName("Should omit playerTag claim when playerTag is blank")
    void shouldOmitPlayerTagClaimWhenBlank() throws Exception {
        UUID accountId = UUID.randomUUID();
        Account account = Account.createNew(accountId, "blanktag@ordago.dev", "hashedPassword");

        UserSession session = UserSession.createNew(
                UUID.randomUUID(),
                accountId,
                "device-2",
                "Chrome",
                "refresh-hash",
                Duration.ofDays(30)
        );

        String token = tokenProvider.createAccessToken(account, session, "   ");

        JsonNode payloadJson = decodeJwtPayload(token);
        assertThat(payloadJson.has("playerTag")).isFalse();

        JsonWebToken jwt = jwtParser.parseOnly(token);
        assertThat(jwt.containsClaim("playerTag")).isFalse();
    }

    @Test
    @DisplayName("Should support default overload createAccessToken(account, session) without playerTag")
    void shouldSupportDefaultOverloadWithoutPlayerTag() throws Exception {
        UUID accountId = UUID.randomUUID();
        Account account = Account.createNew(accountId, "default@ordago.dev", "hashedPassword");

        UserSession session = UserSession.createNew(
                UUID.randomUUID(),
                accountId,
                "device-default",
                "Chrome",
                "refresh-hash",
                Duration.ofDays(30)
        );

        String token = tokenProvider.createAccessToken(account, session);

        JsonNode payloadJson = decodeJwtPayload(token);
        assertThat(payloadJson.has("playerTag")).isFalse();
        assertThat(payloadJson.get("emailVerified").asBoolean()).isFalse();
        assertThat(payloadJson.get("jti").asText()).isNotBlank();

        JsonWebToken jwt = jwtParser.parseOnly(token);
        assertThat(jwt.containsClaim("playerTag")).isFalse();
    }

    private JsonNode decodeJwtPayload(String token) throws Exception {
        String[] parts = token.split("\\.");
        assertThat(parts).hasSize(3);
        byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
        return objectMapper.readTree(payloadBytes);
    }
}
