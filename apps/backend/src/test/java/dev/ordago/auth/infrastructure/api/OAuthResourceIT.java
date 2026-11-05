package dev.ordago.auth.infrastructure.api;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.Mock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.http.Cookie;
import io.restassured.response.Response;
import io.smallrye.jwt.auth.principal.DefaultJWTParser;
import dev.ordago.auth.domain.exception.InvalidOAuthTokenException;
import dev.ordago.auth.domain.model.OAuthProvider;
import dev.ordago.auth.domain.model.OAuthUserInfo;
import dev.ordago.auth.domain.port.out.OAuthVerifierPort;
import dev.ordago.auth.infrastructure.persistence.AccountEntity;
import dev.ordago.auth.infrastructure.persistence.AccountIdentityEntity;
import dev.ordago.auth.infrastructure.persistence.AccountIdentityPanacheRepository;
import dev.ordago.auth.infrastructure.persistence.AccountPanacheRepository;
import dev.ordago.profile.infrastructure.persistence.UserProfileEntity;
import dev.ordago.profile.infrastructure.persistence.UserProfilePanacheRepository;
import dev.ordago.shared.infrastructure.ratelimit.RateLimitService;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
@DisplayName("OAuth & Social Authentication Integration Tests")
class OAuthResourceIT {

    private static final String OAUTH_ENDPOINT = "/api/v1/auth/oauth/{provider}";
    private static final String CLAIM_SOCIAL_ENDPOINT = "/api/v1/auth/guest/claim-social/{provider}";
    private static final String GUEST_AUTH_ENDPOINT = "/api/v1/auth/guest";
    private static final String REGISTER_ENDPOINT = "/api/v1/auth/register";
    private static final String REFRESH_ENDPOINT = "/api/v1/auth/refresh";
    private static final String ME_ENDPOINT = "/api/v1/auth/me";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DefaultJWTParser jwtParser = new DefaultJWTParser();

    @Inject
    RateLimitService rateLimitService;

    @Inject
    AccountPanacheRepository accountPanacheRepository;

    @Inject
    AccountIdentityPanacheRepository accountIdentityPanacheRepository;

    @Inject
    UserProfilePanacheRepository userProfilePanacheRepository;

    /**
     * Deterministic CDI Mock for OAuthVerifierPort avoiding real HTTP network calls to Google/Discord.
     */
    @Mock
    @ApplicationScoped
    public static class MockOAuthVerifierPort implements OAuthVerifierPort {

        private static final Map<String, OAuthUserInfo> tokenToUserInfo = new ConcurrentHashMap<>();
        private static final Map<String, RuntimeException> tokenToException = new ConcurrentHashMap<>();

        public static void reset() {
            tokenToUserInfo.clear();
            tokenToException.clear();
        }

        public static void register(String token, OAuthUserInfo userInfo) {
            tokenToUserInfo.put(token, userInfo);
        }

        public static void registerException(String token, RuntimeException exception) {
            tokenToException.put(token, exception);
        }

        @Override
        public OAuthUserInfo verify(OAuthProvider provider, String token) {
            if (tokenToException.containsKey(token)) {
                throw tokenToException.get(token);
            }
            if (tokenToUserInfo.containsKey(token)) {
                OAuthUserInfo info = tokenToUserInfo.get(token);
                if (info.provider() != provider) {
                    throw new InvalidOAuthTokenException("Provider mismatch: expected " + info.provider() + " but got " + provider);
                }
                return info;
            }
            throw new InvalidOAuthTokenException("Invalid OAuth token");
        }
    }

    @BeforeEach
    void setUp() {
        rateLimitService.clearCache();
        MockOAuthVerifierPort.reset();
        accountPanacheRepository.getEntityManager().clear();
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private JsonNode decodeJwtPayload(String token) {
        assertThat(token).as("JWT token must not be null or blank").isNotBlank();
        String[] parts = token.split("\\.");
        assertThat(parts).as("JWT token must consist of 3 parts (header.payload.signature)").hasSize(3);
        byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
        try {
            return objectMapper.readTree(payloadBytes);
        } catch (Exception e) {
            throw new AssertionError("Failed to parse JWT payload JSON", e);
        }
    }

    private void assertRegularJwtClaims(String token, String expectedPlayerTag, String expectedEmail) {
        assertThat(token).as("JWT token must not be null or blank").isNotBlank();
        JsonNode payload = decodeJwtPayload(token);

        assertThat(payload.hasNonNull("isGuest"))
                .as("JWT payload must contain non-null 'isGuest' claim").isTrue();
        assertThat(payload.get("isGuest").asBoolean())
                .as("Regular JWT must have isGuest=false").isFalse();

        if (expectedPlayerTag != null) {
            assertThat(payload.hasNonNull("playerTag"))
                    .as("Regular JWT must contain non-null 'playerTag' claim").isTrue();
            assertThat(payload.get("playerTag").asText())
                    .as("JWT playerTag claim must match expected tag").isEqualTo(expectedPlayerTag);
        }

        if (expectedEmail != null) {
            assertThat(payload.hasNonNull("email"))
                    .as("Regular JWT must contain non-null 'email' claim").isTrue();
            assertThat(payload.get("email").asText())
                    .as("Regular JWT email must match expected email").isEqualTo(expectedEmail);
        }

        assertThat(payload.hasNonNull("roles"))
                .as("JWT payload must contain non-null 'roles' claim").isTrue();
        JsonNode rolesNode = payload.get("roles");
        boolean hasUserRole = false;
        boolean hasGuestRole = false;
        for (JsonNode role : rolesNode) {
            if ("USER".equals(role.asText())) {
                hasUserRole = true;
            }
            if ("GUEST".equals(role.asText())) {
                hasGuestRole = true;
            }
        }
        assertThat(hasUserRole).as("Roles array must contain 'USER'").isTrue();
        assertThat(hasGuestRole).as("Roles array must NOT contain 'GUEST'").isFalse();

        try {
            JsonWebToken jwt = jwtParser.parseOnly(token);
            if (expectedPlayerTag != null) {
                assertThat(jwt.getClaim("playerTag").toString().replace("\"", ""))
                        .isEqualTo(expectedPlayerTag);
            }
            assertThat(Boolean.parseBoolean(jwt.getClaim("isGuest").toString())).isFalse();
            assertThat(jwt.getGroups()).contains("USER");
            assertThat(jwt.getGroups()).doesNotContain("GUEST");
        } catch (Exception e) {
            throw new AssertionError("Failed to parse regular token with DefaultJWTParser", e);
        }
    }

    private void assertAuthCookies(Response response, String expectedAccessToken, String expectedRefreshToken) {
        Cookie accessCookie = response.getDetailedCookie("access_token");
        assertThat(accessCookie).as("access_token cookie must be present").isNotNull();
        if (expectedAccessToken != null) {
            assertThat(accessCookie.getValue()).isEqualTo(expectedAccessToken);
        }
        assertThat(accessCookie.isHttpOnly()).as("access_token cookie must have HttpOnly flag").isTrue();
        assertThat(accessCookie.getPath()).isEqualTo("/");

        Cookie refreshCookie = response.getDetailedCookie("refresh_token");
        assertThat(refreshCookie).as("refresh_token cookie must be present").isNotNull();
        if (expectedRefreshToken != null) {
            assertThat(refreshCookie.getValue()).isEqualTo(expectedRefreshToken);
        }
        assertThat(refreshCookie.isHttpOnly()).as("refresh_token cookie must have HttpOnly flag").isTrue();
        assertThat(refreshCookie.getPath()).isEqualTo("/api/v1/auth");
    }

    private Response createGuest(String clientIp) {
        return given()
                .header("X-Forwarded-For", clientIp)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "deviceId", "device-guest-" + UUID.randomUUID(),
                        "deviceName", "Linux Test Runner"
                ))
                .when()
                .post(GUEST_AUTH_ENDPOINT)
                .then()
                .statusCode(201)
                .extract().response();
    }

    private Response registerRegularUser(String clientIp, String email, String gameName, String tagLine) {
        return given()
                .header("X-Forwarded-For", clientIp)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "email", email,
                        "password", "SecurePassword123!",
                        "gameName", gameName,
                        "tagLine", tagLine
                ))
                .when()
                .post(REGISTER_ENDPOINT)
                .then()
                .statusCode(201)
                .extract().response();
    }

    // =========================================================================
    // 1. POST /api/v1/auth/oauth/{provider}
    // =========================================================================

    @Nested
    @DisplayName("POST /api/v1/auth/oauth/{provider}")
    class OAuthLoginEndpointTests {

        @Test
        @DisplayName("Returns 400 Bad Request when provider is unsupported (github, invalid)")
        void shouldRejectOAuthLoginWhenProviderIsNotSupported() {
            // Arrange
            String clientIp = "192.168.1.10";
            Map<String, Object> payload = Map.of("token", "dummy_token");

            // Act & Assert - github
            given()
                    .header("X-Forwarded-For", clientIp)
                    .contentType(ContentType.JSON)
                    .pathParam("provider", "github")
                    .body(payload)
                    .when()
                    .post(OAUTH_ENDPOINT)
                    .then()
                    .statusCode(400)
                    .body("code", equalTo("OAUTH_PROVIDER_NOT_SUPPORTED"))
                    .body("status", equalTo(400));

            // Act & Assert - invalid
            given()
                    .header("X-Forwarded-For", clientIp)
                    .contentType(ContentType.JSON)
                    .pathParam("provider", "invalid")
                    .body(payload)
                    .when()
                    .post(OAUTH_ENDPOINT)
                    .then()
                    .statusCode(400)
                    .body("code", equalTo("OAUTH_PROVIDER_NOT_SUPPORTED"))
                    .body("status", equalTo(400));
        }

        @Test
        @DisplayName("Returns 400 Bad Request when token is blank")
        void shouldRejectOAuthLoginWhenTokenIsBlank() {
            // Arrange
            String clientIp = "192.168.1.11";
            Map<String, Object> payload = Map.of("token", "   ");

            // Act & Assert
            given()
                    .header("X-Forwarded-For", clientIp)
                    .contentType(ContentType.JSON)
                    .pathParam("provider", "google")
                    .body(payload)
                    .when()
                    .post(OAUTH_ENDPOINT)
                    .then()
                    .statusCode(400);
        }

        @Test
        @DisplayName("Returns 401 Unauthorized (INVALID_OAUTH_TOKEN) when Google token is invalid")
        void shouldRejectOAuthLoginWhenGoogleTokenIsInvalid() {
            // Arrange
            String clientIp = "192.168.1.12";
            String badToken = "invalid_google_token_" + UUID.randomUUID();
            MockOAuthVerifierPort.registerException(badToken, new InvalidOAuthTokenException("Token verification failed"));

            Map<String, Object> payload = Map.of(
                    "token", badToken,
                    "deviceId", "device-test-1",
                    "deviceName", "Chrome Desktop"
            );

            // Act & Assert
            given()
                    .header("X-Forwarded-For", clientIp)
                    .contentType(ContentType.JSON)
                    .pathParam("provider", "google")
                    .body(payload)
                    .when()
                    .post(OAUTH_ENDPOINT)
                    .then()
                    .statusCode(401)
                    .body("code", equalTo("INVALID_OAUTH_TOKEN"))
                    .body("status", equalTo(401));
        }

        @Test
        @DisplayName("Returns 401 Unauthorized (INVALID_OAUTH_TOKEN) when Discord token is invalid")
        void shouldRejectOAuthLoginWhenDiscordTokenIsInvalid() {
            // Arrange
            String clientIp = "192.168.1.13";
            String badToken = "invalid_discord_token_" + UUID.randomUUID();
            MockOAuthVerifierPort.registerException(badToken, new InvalidOAuthTokenException("Invalid Discord token"));

            Map<String, Object> payload = Map.of("token", badToken);

            // Act & Assert
            given()
                    .header("X-Forwarded-For", clientIp)
                    .contentType(ContentType.JSON)
                    .pathParam("provider", "discord")
                    .body(payload)
                    .when()
                    .post(OAUTH_ENDPOINT)
                    .then()
                    .statusCode(401)
                    .body("code", equalTo("INVALID_OAUTH_TOKEN"))
                    .body("status", equalTo(401));
        }

        @Test
        @DisplayName("Returns 200 OK for new Google user: creates Account, UserProfile, AccountIdentity, USER role, isGuest=false, HttpOnly cookies")
        void shouldAuthenticateNewUserWithGoogleOAuthSuccessfully() {
            // Arrange
            String clientIp = "192.168.1.14";
            String uniqueId = UUID.randomUUID().toString();
            String token = "valid_google_token_" + uniqueId;
            String googleSub = "google_sub_" + uniqueId;
            String email = "google_new_user_" + uniqueId + "@ordago.dev";
            String displayName = "GoogleMusMaster";

            OAuthUserInfo mockUserInfo = new OAuthUserInfo(
                    OAuthProvider.GOOGLE,
                    googleSub,
                    email,
                    true,
                    displayName,
                    "https://lh3.googleusercontent.com/avatar.png"
            );
            MockOAuthVerifierPort.register(token, mockUserInfo);

            Map<String, Object> payload = Map.of(
                    "token", token,
                    "deviceId", "device-google-new",
                    "deviceName", "Android Chrome"
            );

            // Act
            Response response = given()
                    .header("X-Forwarded-For", clientIp)
                    .contentType(ContentType.JSON)
                    .pathParam("provider", "google")
                    .body(payload)
                    .when()
                    .post(OAUTH_ENDPOINT)
                    .then()
                    .statusCode(200)
                    .body("accessToken", notNullValue())
                    .body("refreshToken", notNullValue())
                    .body("tokenType", equalTo("Bearer"))
                    .body("user.id", notNullValue())
                    .body("user.email", equalTo(email))
                    .body("user.emailVerified", equalTo(true))
                    .body("user.isSystemAdmin", equalTo(false))
                    .body("user.gameName", equalTo(displayName))
                    .body("user.tagLine", notNullValue())
                    .body("user.playerTag", matchesPattern("^GoogleMusMaster#[0-9A-Z]{3,4}$"))
                    .body("user.eloRating", equalTo(1200))
                    .extract().response();

            // Assert - JWT Claims
            String accessToken = response.path("accessToken");
            String refreshToken = response.path("refreshToken");
            String playerTag = response.path("user.playerTag");
            String userIdStr = response.path("user.id");
            UUID accountId = UUID.fromString(userIdStr);

            assertRegularJwtClaims(accessToken, playerTag, email);

            // Assert - HttpOnly Cookies
            assertAuthCookies(response, accessToken, refreshToken);

            // Assert - Database Persistence
            AccountEntity accountEntity = accountPanacheRepository.findById(accountId);
            assertThat(accountEntity).isNotNull();
            assertThat(accountEntity.email).isEqualTo(email);
            assertThat(accountEntity.isGuest).isFalse();
            assertThat(accountEntity.emailVerified).isTrue();
            assertThat(accountEntity.passwordHash).isNull();

            UserProfileEntity profileEntity = userProfilePanacheRepository.findById(accountId);
            assertThat(profileEntity).isNotNull();
            assertThat(profileEntity.gameName).isEqualTo(displayName);
            assertThat(profileEntity.gameName + "#" + profileEntity.tagLine).isEqualTo(playerTag);

            List<AccountIdentityEntity> identities = accountIdentityPanacheRepository.findByAccountId(accountId);
            assertThat(identities).hasSize(1);
            AccountIdentityEntity identity = identities.get(0);
            assertThat(identity.provider).isEqualTo("GOOGLE");
            assertThat(identity.providerSubject).isEqualTo(googleSub);
            assertThat(identity.email).isEqualTo(email);
        }

        @Test
        @DisplayName("Returns 200 OK for new Discord user: creates Account, UserProfile, AccountIdentity, USER role, isGuest=false")
        void shouldAuthenticateNewUserWithDiscordOAuthSuccessfully() {
            // Arrange
            String clientIp = "192.168.1.15";
            String uniqueId = UUID.randomUUID().toString();
            String token = "valid_discord_token_" + uniqueId;
            String discordSub = "discord_sub_" + uniqueId;
            String email = "discord_new_user_" + uniqueId + "@ordago.dev";
            String displayName = "DiscordPlayer";

            OAuthUserInfo mockUserInfo = new OAuthUserInfo(
                    OAuthProvider.DISCORD,
                    discordSub,
                    email,
                    true,
                    displayName,
                    "https://cdn.discordapp.com/avatars/user.png"
            );
            MockOAuthVerifierPort.register(token, mockUserInfo);

            Map<String, Object> payload = Map.of(
                    "token", token,
                    "deviceId", "device-discord-new",
                    "deviceName", "Discord Desktop App"
            );

            // Act
            Response response = given()
                    .header("X-Forwarded-For", clientIp)
                    .contentType(ContentType.JSON)
                    .pathParam("provider", "discord")
                    .body(payload)
                    .when()
                    .post(OAUTH_ENDPOINT)
                    .then()
                    .statusCode(200)
                    .body("accessToken", notNullValue())
                    .body("refreshToken", notNullValue())
                    .body("user.id", notNullValue())
                    .body("user.email", equalTo(email))
                    .body("user.gameName", equalTo(displayName))
                    .body("user.playerTag", matchesPattern("^DiscordPlayer#[0-9A-Z]{3,4}$"))
                    .extract().response();

            // Assert - JWT Claims & DB
            String accessToken = response.path("accessToken");
            String refreshToken = response.path("refreshToken");
            String playerTag = response.path("user.playerTag");
            UUID accountId = UUID.fromString(response.path("user.id"));

            assertRegularJwtClaims(accessToken, playerTag, email);
            assertAuthCookies(response, accessToken, refreshToken);

            List<AccountIdentityEntity> identities = accountIdentityPanacheRepository.findByAccountId(accountId);
            assertThat(identities).hasSize(1);
            assertThat(identities.get(0).provider).isEqualTo("DISCORD");
            assertThat(identities.get(0).providerSubject).isEqualTo(discordSub);
        }

        @Test
        @DisplayName("Returns 200 OK for existing email user without prior identity: links identity and marks emailVerified=true")
        void shouldLinkSocialIdentityToExistingEmailUserAndMarkEmailVerified() {
            // Arrange 1: Register regular user with unverified email
            String clientIp = "192.168.1.16";
            String uniqueId = UUID.randomUUID().toString();
            String email = "existing_user_" + uniqueId + "@ordago.dev";
            String gameName = "RegularPlayer";
            String tagLine = "REG1";

            Response registerResponse = registerRegularUser(clientIp, email, gameName, tagLine);
            String registeredUserId = registerResponse.path("user.id");
            String registeredPlayerTag = registerResponse.path("user.playerTag");
            UUID accountId = UUID.fromString(registeredUserId);

            // Verify initial state: emailVerified is false, no account identities
            AccountEntity initialAccount = accountPanacheRepository.findById(accountId);
            assertThat(initialAccount.emailVerified).isFalse();
            assertThat(accountIdentityPanacheRepository.findByAccountId(accountId)).isEmpty();

            // Arrange 2: Prepare OAuth response for same email
            String token = "valid_oauth_existing_email_" + uniqueId;
            String googleSub = "google_sub_existing_" + uniqueId;
            OAuthUserInfo mockUserInfo = new OAuthUserInfo(
                    OAuthProvider.GOOGLE,
                    googleSub,
                    email,
                    true, // social provider verified the email
                    "Google Name",
                    null
            );
            MockOAuthVerifierPort.register(token, mockUserInfo);

            // Act: Authenticate with Google
            Response oauthResponse = given()
                    .header("X-Forwarded-For", clientIp)
                    .contentType(ContentType.JSON)
                    .pathParam("provider", "google")
                    .body(Map.of("token", token))
                    .when()
                    .post(OAUTH_ENDPOINT)
                    .then()
                    .statusCode(200)
                    .body("user.id", equalTo(registeredUserId))
                    .body("user.email", equalTo(email))
                    .body("user.emailVerified", equalTo(true))
                    .body("user.playerTag", equalTo(registeredPlayerTag))
                    .extract().response();

            // Assert: Identity linked to existing account and emailVerified marked true
            String accessToken = oauthResponse.path("accessToken");
            assertRegularJwtClaims(accessToken, registeredPlayerTag, email);

            accountPanacheRepository.getEntityManager().clear();
            AccountEntity updatedAccount = accountPanacheRepository.findById(accountId);
            assertThat(updatedAccount.emailVerified).isTrue();

            List<AccountIdentityEntity> identities = accountIdentityPanacheRepository.findByAccountId(accountId);
            assertThat(identities).hasSize(1);
            assertThat(identities.get(0).provider).isEqualTo("GOOGLE");
            assertThat(identities.get(0).providerSubject).isEqualTo(googleSub);
            assertThat(identities.get(0).accountId).isEqualTo(accountId);
        }

        @Test
        @DisplayName("Returns 200 OK for user with already linked identity: authenticates directly without creating duplicate identity")
        void shouldAuthenticateExistingSocialUserWithoutCreatingDuplicateIdentity() {
            // Arrange: Authenticate user once via Google OAuth
            String clientIp = "192.168.1.17";
            String uniqueId = UUID.randomUUID().toString();
            String token1 = "token_step1_" + uniqueId;
            String token2 = "token_step2_" + uniqueId;
            String googleSub = "google_sub_linked_" + uniqueId;
            String email = "linked_user_" + uniqueId + "@ordago.dev";

            OAuthUserInfo mockUserInfo = new OAuthUserInfo(
                    OAuthProvider.GOOGLE,
                    googleSub,
                    email,
                    true,
                    "RepeatUser",
                    null
            );
            MockOAuthVerifierPort.register(token1, mockUserInfo);
            MockOAuthVerifierPort.register(token2, mockUserInfo);

            Response firstResponse = given()
                    .header("X-Forwarded-For", clientIp)
                    .contentType(ContentType.JSON)
                    .pathParam("provider", "google")
                    .body(Map.of("token", token1))
                    .when()
                    .post(OAUTH_ENDPOINT)
                    .then()
                    .statusCode(200)
                    .extract().response();

            String userIdStr = firstResponse.path("user.id");
            String playerTag = firstResponse.path("user.playerTag");
            UUID accountId = UUID.fromString(userIdStr);

            // Verify initial identity count is 1
            List<AccountIdentityEntity> initialIdentities = accountIdentityPanacheRepository.findByAccountId(accountId);
            assertThat(initialIdentities).hasSize(1);

            // Act: Authenticate a second time with same identity provider & subject
            Response secondResponse = given()
                    .header("X-Forwarded-For", clientIp)
                    .contentType(ContentType.JSON)
                    .pathParam("provider", "google")
                    .body(Map.of("token", token2))
                    .when()
                    .post(OAUTH_ENDPOINT)
                    .then()
                    .statusCode(200)
                    .body("user.id", equalTo(userIdStr))
                    .body("user.email", equalTo(email))
                    .body("user.playerTag", equalTo(playerTag))
                    .extract().response();

            // Assert: No duplicates created
            List<AccountIdentityEntity> postIdentities = accountIdentityPanacheRepository.findByAccountId(accountId);
            assertThat(postIdentities).hasSize(1);

            String secondAccessToken = secondResponse.path("accessToken");
            assertRegularJwtClaims(secondAccessToken, playerTag, email);
        }
    }

    // =========================================================================
    // 2. POST /api/v1/auth/guest/claim-social/{provider}
    // =========================================================================

    @Nested
    @DisplayName("POST /api/v1/auth/guest/claim-social/{provider}")
    class ClaimGuestSocialEndpointTests {

        @Test
        @DisplayName("Returns 401 Unauthorized when request lacks Authorization header")
        void shouldRejectClaimSocialWithoutAuthorizationHeader() {
            // Arrange
            String clientIp = "192.168.1.20";
            Map<String, Object> payload = Map.of("token", "dummy_token");

            // Act & Assert
            given()
                    .header("X-Forwarded-For", clientIp)
                    .contentType(ContentType.JSON)
                    .pathParam("provider", "google")
                    .body(payload)
                    .when()
                    .post(CLAIM_SOCIAL_ENDPOINT)
                    .then()
                    .statusCode(401);
        }

        @Test
        @DisplayName("Returns 400 Bad Request when provider is unsupported")
        void shouldRejectClaimSocialWhenProviderIsNotSupported() {
            // Arrange: Create guest account
            String clientIp = "192.168.1.21";
            Response guestResponse = createGuest(clientIp);
            String guestAccessToken = guestResponse.path("accessToken");

            // Act & Assert
            given()
                    .header("X-Forwarded-For", clientIp)
                    .header("Authorization", "Bearer " + guestAccessToken)
                    .contentType(ContentType.JSON)
                    .pathParam("provider", "unsupported_provider")
                    .body(Map.of("token", "any_token"))
                    .when()
                    .post(CLAIM_SOCIAL_ENDPOINT)
                    .then()
                    .statusCode(400)
                    .body("code", equalTo("OAUTH_PROVIDER_NOT_SUPPORTED"))
                    .body("status", equalTo(400));
        }

        @Test
        @DisplayName("Returns 401 Unauthorized (INVALID_OAUTH_TOKEN) when OAuth token is invalid")
        void shouldRejectClaimSocialWhenOAuthTokenIsInvalid() {
            // Arrange: Create guest account
            String clientIp = "192.168.1.22";
            Response guestResponse = createGuest(clientIp);
            String guestAccessToken = guestResponse.path("accessToken");

            String invalidToken = "invalid_claim_token_" + UUID.randomUUID();
            MockOAuthVerifierPort.registerException(invalidToken, new InvalidOAuthTokenException("Invalid token"));

            // Act & Assert
            given()
                    .header("X-Forwarded-For", clientIp)
                    .header("Authorization", "Bearer " + guestAccessToken)
                    .contentType(ContentType.JSON)
                    .pathParam("provider", "google")
                    .body(Map.of("token", invalidToken))
                    .when()
                    .post(CLAIM_SOCIAL_ENDPOINT)
                    .then()
                    .statusCode(401)
                    .body("code", equalTo("INVALID_OAUTH_TOKEN"))
                    .body("status", equalTo(401));
        }

        @Test
        @DisplayName("Returns 400 Bad Request (ACCOUNT_NOT_GUEST) when regular user attempts to claim")
        void shouldRejectClaimSocialWhenUserIsNotAGuest() {
            // Arrange: Register a regular user
            String clientIp = "192.168.1.23";
            String uniqueId = UUID.randomUUID().toString();
            String regularEmail = "reg_claim_" + uniqueId + "@ordago.dev";

            Response registerResponse = registerRegularUser(clientIp, regularEmail, "RegUser", "R001");
            String regularAccessToken = registerResponse.path("accessToken");

            String token = "valid_token_reg_claim_" + uniqueId;
            OAuthUserInfo mockUserInfo = new OAuthUserInfo(
                    OAuthProvider.GOOGLE,
                    "google_sub_reg_" + uniqueId,
                    "social_for_reg_" + uniqueId + "@ordago.dev",
                    true,
                    "SocialName",
                    null
            );
            MockOAuthVerifierPort.register(token, mockUserInfo);

            // Act & Assert
            given()
                    .header("X-Forwarded-For", clientIp)
                    .header("Authorization", "Bearer " + regularAccessToken)
                    .contentType(ContentType.JSON)
                    .pathParam("provider", "google")
                    .body(Map.of("token", token))
                    .when()
                    .post(CLAIM_SOCIAL_ENDPOINT)
                    .then()
                    .statusCode(400)
                    .body("code", equalTo("ACCOUNT_NOT_GUEST"))
                    .body("status", equalTo(400));
        }

        @Test
        @DisplayName("Returns 409 Conflict (OAUTH_IDENTITY_ALREADY_LINKED) when social identity is already linked to another account")
        void shouldRejectClaimSocialWhenIdentityAlreadyLinkedToAnotherAccount() {
            // Arrange 1: User A authenticates via Google
            String clientIp = "192.168.1.24";
            String uniqueId = UUID.randomUUID().toString();
            String tokenUserA = "token_user_a_" + uniqueId;
            String sharedGoogleSub = "shared_google_sub_" + uniqueId;
            String emailUserA = "user_a_" + uniqueId + "@ordago.dev";

            OAuthUserInfo userInfoA = new OAuthUserInfo(
                    OAuthProvider.GOOGLE,
                    sharedGoogleSub,
                    emailUserA,
                    true,
                    "UserA",
                    null
            );
            MockOAuthVerifierPort.register(tokenUserA, userInfoA);

            given()
                    .header("X-Forwarded-For", clientIp)
                    .contentType(ContentType.JSON)
                    .pathParam("provider", "google")
                    .body(Map.of("token", tokenUserA))
                    .when()
                    .post(OAUTH_ENDPOINT)
                    .then()
                    .statusCode(200);

            // Arrange 2: Guest B created
            Response guestResponse = createGuest(clientIp);
            String guestAccessToken = guestResponse.path("accessToken");

            // Guest B attempts to claim with user A's Google credentials
            String tokenClaimConflict = "token_claim_conflict_" + uniqueId;
            OAuthUserInfo userInfoConflict = new OAuthUserInfo(
                    OAuthProvider.GOOGLE,
                    sharedGoogleSub, // same Google subject!
                    "distinct_email_" + uniqueId + "@ordago.dev",
                    true,
                    "ConflictName",
                    null
            );
            MockOAuthVerifierPort.register(tokenClaimConflict, userInfoConflict);

            // Act & Assert: Should fail with 409 OAUTH_IDENTITY_ALREADY_LINKED
            given()
                    .header("X-Forwarded-For", clientIp)
                    .header("Authorization", "Bearer " + guestAccessToken)
                    .contentType(ContentType.JSON)
                    .pathParam("provider", "google")
                    .body(Map.of("token", tokenClaimConflict))
                    .when()
                    .post(CLAIM_SOCIAL_ENDPOINT)
                    .then()
                    .statusCode(409)
                    .body("code", equalTo("OAUTH_IDENTITY_ALREADY_LINKED"))
                    .body("status", equalTo(409));
        }

        @Test
        @DisplayName("Returns 409 Conflict (EMAIL_ALREADY_EXISTS) when social email collides with another registered account")
        void shouldRejectClaimSocialWhenSocialEmailCollidesWithExistingAccount() {
            // Arrange 1: Register User A with regular email
            String clientIp = "192.168.1.25";
            String uniqueId = UUID.randomUUID().toString();
            String collisionEmail = "collision_owner_" + uniqueId + "@ordago.dev";
            registerRegularUser(clientIp, collisionEmail, "ColOwner", "COL1");

            // Arrange 2: Create Guest B
            Response guestResponse = createGuest(clientIp);
            String guestAccessToken = guestResponse.path("accessToken");

            // Guest B attempts to claim with a social account that has User A's email
            String tokenClaim = "token_collision_" + uniqueId;
            OAuthUserInfo mockUserInfo = new OAuthUserInfo(
                    OAuthProvider.GOOGLE,
                    "new_distinct_google_sub_" + uniqueId,
                    collisionEmail, // matches User A's email!
                    true,
                    "ColClaimer",
                    null
            );
            MockOAuthVerifierPort.register(tokenClaim, mockUserInfo);

            // Act & Assert: Should fail with 409 EMAIL_ALREADY_EXISTS
            given()
                    .header("X-Forwarded-For", clientIp)
                    .header("Authorization", "Bearer " + guestAccessToken)
                    .contentType(ContentType.JSON)
                    .pathParam("provider", "google")
                    .body(Map.of("token", tokenClaim))
                    .when()
                    .post(CLAIM_SOCIAL_ENDPOINT)
                    .then()
                    .statusCode(409)
                    .body("code", equalTo("EMAIL_ALREADY_EXISTS"))
                    .body("status", equalTo(409));
        }

        @Test
        @DisplayName("Successful claim with Google: 200 OK, preserves profile, isGuest=false, revokes old guest session, emits USER JWT")
        void shouldClaimGuestWithGoogleSuccessfullyPreservingProfileAndRevokingOldSessions() {
            // Arrange 1: Create Guest account
            String clientIp = "192.168.1.26";
            String uniqueId = UUID.randomUUID().toString();
            Response guestResponse = createGuest(clientIp);

            String guestAccessToken = guestResponse.path("accessToken");
            String guestRefreshToken = guestResponse.path("refreshToken");
            String guestUserId = guestResponse.path("user.id");
            String originalGameName = guestResponse.path("user.gameName");
            String originalTagLine = guestResponse.path("user.tagLine");
            String originalPlayerTag = guestResponse.path("user.playerTag");
            int originalElo = guestResponse.path("user.eloRating");
            UUID accountId = UUID.fromString(guestUserId);

            assertThat(originalGameName).matches("^Guest[0-9]{4}$");
            assertThat(originalTagLine).isEqualTo("666");
            assertThat(originalPlayerTag).isEqualTo(originalGameName + "#666");

            // Arrange 2: Prepare valid Google OAuth response
            String claimToken = "valid_claim_google_token_" + uniqueId;
            String googleSub = "google_sub_claim_" + uniqueId;
            String socialEmail = "claimed_google_" + uniqueId + "@ordago.dev";

            OAuthUserInfo mockUserInfo = new OAuthUserInfo(
                    OAuthProvider.GOOGLE,
                    googleSub,
                    socialEmail,
                    true,
                    "Google Display Name",
                    null
            );
            MockOAuthVerifierPort.register(claimToken, mockUserInfo);

            Map<String, Object> payload = Map.of(
                    "token", claimToken,
                    "deviceId", "device-claimed-google",
                    "deviceName", "Claimed Google Device"
            );

            // Act: Claim guest account with Google
            Response claimResponse = given()
                    .header("X-Forwarded-For", clientIp)
                    .header("Authorization", "Bearer " + guestAccessToken)
                    .contentType(ContentType.JSON)
                    .pathParam("provider", "google")
                    .body(payload)
                    .when()
                    .post(CLAIM_SOCIAL_ENDPOINT)
                    .then()
                    .statusCode(200)
                    .body("accessToken", notNullValue())
                    .body("refreshToken", notNullValue())
                    .body("tokenType", equalTo("Bearer"))
                    .body("user.id", equalTo(guestUserId))
                    .body("user.email", equalTo(socialEmail))
                    .body("user.emailVerified", equalTo(true))
                    .body("user.gameName", equalTo(originalGameName))
                    .body("user.tagLine", equalTo(originalTagLine))
                    .body("user.playerTag", equalTo(originalPlayerTag))
                    .body("user.eloRating", equalTo(originalElo))
                    .extract().response();

            // Assert 1: Profile preservation in response & JWT
            String claimedAccessToken = claimResponse.path("accessToken");
            String claimedRefreshToken = claimResponse.path("refreshToken");
            assertThat(claimedAccessToken).isNotEqualTo(guestAccessToken);

            assertRegularJwtClaims(claimedAccessToken, originalPlayerTag, socialEmail);
            assertAuthCookies(claimResponse, claimedAccessToken, claimedRefreshToken);

            // Assert 2: Database Account state updated to regular (isGuest=false)
            AccountEntity accountEntity = accountPanacheRepository.findById(accountId);
            assertThat(accountEntity).isNotNull();
            assertThat(accountEntity.email).isEqualTo(socialEmail);
            assertThat(accountEntity.isGuest).isFalse();
            assertThat(accountEntity.emailVerified).isTrue();
            assertThat(accountEntity.passwordHash).isNull();

            // Assert 3: UserProfile preserved intact in DB
            UserProfileEntity userProfile = userProfilePanacheRepository.findById(accountId);
            assertThat(userProfile).isNotNull();
            assertThat(userProfile.gameName).isEqualTo(originalGameName);
            assertThat(userProfile.tagLine).isEqualTo(originalTagLine);
            assertThat(userProfile.gameName + "#" + userProfile.tagLine).isEqualTo(originalPlayerTag);
            assertThat(userProfile.eloRating).isEqualTo(originalElo);

            // Assert 4: AccountIdentity created for Google
            List<AccountIdentityEntity> identities = accountIdentityPanacheRepository.findByAccountId(accountId);
            assertThat(identities).hasSize(1);
            AccountIdentityEntity identity = identities.get(0);
            assertThat(identity.provider).isEqualTo("GOOGLE");
            assertThat(identity.providerSubject).isEqualTo(googleSub);
            assertThat(identity.email).isEqualTo(socialEmail);

            // Assert 5: Prior guest session was revoked (cannot refresh with old token)
            given()
                    .header("X-Forwarded-For", clientIp)
                    .contentType(ContentType.JSON)
                    .body(Map.of("refreshToken", guestRefreshToken))
                    .when()
                    .post(REFRESH_ENDPOINT)
                    .then()
                    .statusCode(401)
                    .body("code", equalTo("SESSION_REVOKED"));

            // Assert 6: GET /me confirms user is regular and preserved
            given()
                    .header("X-Forwarded-For", clientIp)
                    .header("Authorization", "Bearer " + claimedAccessToken)
                    .when()
                    .get(ME_ENDPOINT)
                    .then()
                    .statusCode(200)
                    .body("id", equalTo(guestUserId))
                    .body("email", equalTo(socialEmail))
                    .body("gameName", equalTo(originalGameName))
                    .body("tagLine", equalTo(originalTagLine))
                    .body("playerTag", equalTo(originalPlayerTag))
                    .body("eloRating", equalTo(originalElo));
        }

        @Test
        @DisplayName("Successful claim with Discord: 200 OK, preserves profile, isGuest=false, revokes old guest session, emits USER JWT")
        void shouldClaimGuestWithDiscordSuccessfullyPreservingProfileAndRevokingOldSessions() {
            // Arrange 1: Create Guest account
            String clientIp = "192.168.1.27";
            String uniqueId = UUID.randomUUID().toString();
            Response guestResponse = createGuest(clientIp);

            String guestAccessToken = guestResponse.path("accessToken");
            String guestRefreshToken = guestResponse.path("refreshToken");
            String guestUserId = guestResponse.path("user.id");
            String originalGameName = guestResponse.path("user.gameName");
            String originalTagLine = guestResponse.path("user.tagLine");
            String originalPlayerTag = guestResponse.path("user.playerTag");
            int originalElo = guestResponse.path("user.eloRating");
            UUID accountId = UUID.fromString(guestUserId);

            // Arrange 2: Prepare valid Discord OAuth response
            String claimToken = "valid_claim_discord_token_" + uniqueId;
            String discordSub = "discord_sub_claim_" + uniqueId;
            String socialEmail = "claimed_discord_" + uniqueId + "@ordago.dev";

            OAuthUserInfo mockUserInfo = new OAuthUserInfo(
                    OAuthProvider.DISCORD,
                    discordSub,
                    socialEmail,
                    true,
                    "Discord Display Name",
                    "https://cdn.discordapp.com/avatars/claim.png"
            );
            MockOAuthVerifierPort.register(claimToken, mockUserInfo);

            Map<String, Object> payload = Map.of(
                    "token", claimToken,
                    "deviceId", "device-claimed-discord",
                    "deviceName", "Claimed Discord Device"
            );

            // Act: Claim guest account with Discord
            Response claimResponse = given()
                    .header("X-Forwarded-For", clientIp)
                    .header("Authorization", "Bearer " + guestAccessToken)
                    .contentType(ContentType.JSON)
                    .pathParam("provider", "discord")
                    .body(payload)
                    .when()
                    .post(CLAIM_SOCIAL_ENDPOINT)
                    .then()
                    .statusCode(200)
                    .body("accessToken", notNullValue())
                    .body("refreshToken", notNullValue())
                    .body("user.id", equalTo(guestUserId))
                    .body("user.email", equalTo(socialEmail))
                    .body("user.gameName", equalTo(originalGameName))
                    .body("user.tagLine", equalTo(originalTagLine))
                    .body("user.playerTag", equalTo(originalPlayerTag))
                    .body("user.eloRating", equalTo(originalElo))
                    .extract().response();

            // Assert 1: Profile preservation in response & JWT
            String claimedAccessToken = claimResponse.path("accessToken");
            String claimedRefreshToken = claimResponse.path("refreshToken");

            assertRegularJwtClaims(claimedAccessToken, originalPlayerTag, socialEmail);
            assertAuthCookies(claimResponse, claimedAccessToken, claimedRefreshToken);

            // Assert 2: Database Account state updated to regular (isGuest=false)
            AccountEntity accountEntity = accountPanacheRepository.findById(accountId);
            assertThat(accountEntity).isNotNull();
            assertThat(accountEntity.email).isEqualTo(socialEmail);
            assertThat(accountEntity.isGuest).isFalse();

            // Assert 3: AccountIdentity created for Discord
            List<AccountIdentityEntity> identities = accountIdentityPanacheRepository.findByAccountId(accountId);
            assertThat(identities).hasSize(1);
            assertThat(identities.get(0).provider).isEqualTo("DISCORD");
            assertThat(identities.get(0).providerSubject).isEqualTo(discordSub);
            assertThat(identities.get(0).email).isEqualTo(socialEmail);

            // Assert 4: Old guest refresh token was revoked
            given()
                    .header("X-Forwarded-For", clientIp)
                    .contentType(ContentType.JSON)
                    .body(Map.of("refreshToken", guestRefreshToken))
                    .when()
                    .post(REFRESH_ENDPOINT)
                    .then()
                    .statusCode(401)
                    .body("code", equalTo("SESSION_REVOKED"));
        }
    }
}
