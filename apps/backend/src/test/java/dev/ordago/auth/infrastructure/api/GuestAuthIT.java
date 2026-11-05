package dev.ordago.auth.infrastructure.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.http.Cookie;
import io.restassured.response.Response;
import io.smallrye.jwt.auth.principal.DefaultJWTParser;
import jakarta.inject.Inject;
import dev.ordago.shared.infrastructure.ratelimit.RateLimitService;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

@QuarkusTest
@DisplayName("Guest Authentication & Account Claim Integration Tests")
class GuestAuthIT {

    private static final String GUEST_AUTH_ENDPOINT = "/api/v1/auth/guest";
    private static final String GUEST_CLAIM_ENDPOINT = "/api/v1/auth/guest/claim";
    private static final String REGISTER_ENDPOINT = "/api/v1/auth/register";
    private static final String LOGIN_ENDPOINT = "/api/v1/auth/login";
    private static final String ME_ENDPOINT = "/api/v1/auth/me";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DefaultJWTParser jwtParser = new DefaultJWTParser();

    @Inject
    RateLimitService rateLimitService;

    @Inject
    DataSource dataSource;

    @BeforeEach
    void setUp() {
        rateLimitService.clearCache();
    }

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

    private void assertGuestJwtClaims(String token, String expectedPlayerTag) {
        assertThat(token).as("JWT token must not be null or blank").isNotBlank();
        JsonNode payload = decodeJwtPayload(token);

        assertThat(payload.hasNonNull("isGuest"))
                .as("JWT payload must contain non-null 'isGuest' claim").isTrue();
        assertThat(payload.get("isGuest").isBoolean())
                .as("'isGuest' claim must be boolean").isTrue();
        assertThat(payload.get("isGuest").asBoolean())
                .as("Guest JWT must have isGuest=true").isTrue();

        assertThat(payload.hasNonNull("emailVerified"))
                .as("JWT payload must contain non-null 'emailVerified' claim").isTrue();
        assertThat(payload.get("emailVerified").asBoolean())
                .as("Guest JWT must have emailVerified=false").isFalse();

        assertThat(payload.hasNonNull("playerTag"))
                .as("JWT payload must contain non-null 'playerTag' claim").isTrue();
        assertThat(payload.get("playerTag").asText())
                .as("Guest playerTag must match expected tag").isEqualTo(expectedPlayerTag);
        assertThat(payload.get("playerTag").asText())
                .as("Guest playerTag must follow 'GuestXXXX#666' pattern")
                .matches("^Guest[0-9]{4}#666$");

        assertThat(payload.hasNonNull("roles"))
                .as("JWT payload must contain non-null 'roles' claim").isTrue();
        JsonNode rolesNode = payload.get("roles");
        assertThat(rolesNode.isArray()).as("'roles' claim must be an array").isTrue();
        boolean hasGuestRole = false;
        for (JsonNode role : rolesNode) {
            if ("GUEST".equals(role.asText())) {
                hasGuestRole = true;
                break;
            }
        }
        assertThat(hasGuestRole).as("Roles array must contain 'GUEST'").isTrue();

        try {
            JsonWebToken jwt = jwtParser.parseOnly(token);
            assertThat(jwt.getClaim("playerTag").toString().replace("\"", ""))
                    .isEqualTo(expectedPlayerTag);
            assertThat(Boolean.parseBoolean(jwt.getClaim("isGuest").toString())).isTrue();
            assertThat(Boolean.parseBoolean(jwt.getClaim("emailVerified").toString())).isFalse();
            assertThat(jwt.getGroups()).contains("GUEST");
        } catch (Exception e) {
            throw new AssertionError("Failed to parse guest token with DefaultJWTParser", e);
        }
    }

    private void assertRegularJwtClaims(String token, String expectedPlayerTag, String expectedEmail) {
        assertThat(token).as("JWT token must not be null or blank").isNotBlank();
        JsonNode payload = decodeJwtPayload(token);

        assertThat(payload.hasNonNull("isGuest"))
                .as("JWT payload must contain non-null 'isGuest' claim").isTrue();
        assertThat(payload.get("isGuest").asBoolean())
                .as("Claimed regular JWT must have isGuest=false").isFalse();

        assertThat(payload.hasNonNull("playerTag"))
                .as("Claimed regular JWT must contain non-null 'playerTag' claim").isTrue();
        assertThat(payload.get("playerTag").asText())
                .as("Claimed playerTag must be preserved").isEqualTo(expectedPlayerTag);

        assertThat(payload.hasNonNull("email"))
                .as("Claimed regular JWT must contain non-null 'email' claim").isTrue();
        assertThat(payload.get("email").asText())
                .as("Claimed regular JWT email must match").isEqualTo(expectedEmail);

        assertThat(payload.hasNonNull("roles"))
                .as("JWT payload must contain non-null 'roles' claim").isTrue();
        JsonNode rolesNode = payload.get("roles");
        boolean hasUserRole = false;
        for (JsonNode role : rolesNode) {
            if ("USER".equals(role.asText())) {
                hasUserRole = true;
                break;
            }
        }
        assertThat(hasUserRole).as("Roles array must contain 'USER'").isTrue();

        try {
            JsonWebToken jwt = jwtParser.parseOnly(token);
            assertThat(jwt.getClaim("playerTag").toString().replace("\"", ""))
                    .isEqualTo(expectedPlayerTag);
            assertThat(Boolean.parseBoolean(jwt.getClaim("isGuest").toString())).isFalse();
            assertThat(jwt.getGroups()).contains("USER");
        } catch (Exception e) {
            throw new AssertionError("Failed to parse regular token with DefaultJWTParser", e);
        }
    }

    // =========================================================================
    // A) POST /api/v1/auth/guest
    // =========================================================================

    @Test
    @DisplayName("POST /guest: Creates guest account with AuthResponse, valid JWT claims, and HttpOnly cookies")
    void shouldCreateGuestAccountSuccessfullyWithAuthResponseAndCookies() {
        // Arrange
        String clientIp = "198.51.100.10";
        Map<String, Object> guestPayload = Map.of(
                "deviceId", "device-guest-test-1",
                "deviceName", "Linux Desktop Runner"
        );

        // Act
        Response response = given()
                .header("X-Forwarded-For", clientIp)
                .contentType(ContentType.JSON)
                .body(guestPayload)
                .when()
                .post(GUEST_AUTH_ENDPOINT)
                .then()
                .statusCode(201)
                .body("accessToken", notNullValue())
                .body("refreshToken", notNullValue())
                .body("tokenType", equalTo("Bearer"))
                .body("user.id", notNullValue())
                .body("user.email", nullValue())
                .body("user.isSystemAdmin", equalTo(false))
                .body("user.emailVerified", equalTo(false))
                .body("user.gameName", matchesPattern("^Guest[0-9]{4}$"))
                .body("user.tagLine", equalTo("666"))
                .body("user.playerTag", matchesPattern("^Guest[0-9]{4}#666$"))
                .body("user.eloRating", equalTo(1200))
                .extract().response();

        // Assert - JWT Claims
        String accessToken = response.path("accessToken");
        String playerTag = response.path("user.playerTag");
        assertGuestJwtClaims(accessToken, playerTag);

        // Assert - HttpOnly Cookies
        Cookie accessCookie = response.getDetailedCookie("access_token");
        assertThat(accessCookie)
                .as("access_token cookie must be present")
                .isNotNull();
        assertThat(accessCookie.getValue()).isEqualTo(accessToken);
        assertThat(accessCookie.isHttpOnly())
                .as("access_token cookie must have HttpOnly flag")
                .isTrue();
        assertThat(accessCookie.getPath())
                .as("access_token cookie path must be '/'")
                .isEqualTo("/");

        Cookie refreshCookie = response.getDetailedCookie("refresh_token");
        assertThat(refreshCookie)
                .as("refresh_token cookie must be present")
                .isNotNull();
        assertThat(refreshCookie.getValue()).isEqualTo(response.path("refreshToken"));
        assertThat(refreshCookie.isHttpOnly())
                .as("refresh_token cookie must have HttpOnly flag")
                .isTrue();
        assertThat(refreshCookie.getPath())
                .as("refresh_token cookie path must be '/api/v1/auth'")
                .isEqualTo("/api/v1/auth");
    }

    @Test
    @DisplayName("POST /guest: Creates guest account successfully when request body is empty")
    void shouldCreateGuestAccountWhenBodyIsEmpty() {
        // Arrange
        String clientIp = "198.51.100.11";

        // Act & Assert
        given()
                .header("X-Forwarded-For", clientIp)
                .contentType(ContentType.JSON)
                .body("{}")
                .when()
                .post(GUEST_AUTH_ENDPOINT)
                .then()
                .statusCode(201)
                .body("accessToken", notNullValue())
                .body("refreshToken", notNullValue())
                .body("user.playerTag", matchesPattern("^Guest[0-9]{4}#666$"));
    }

    @Test
    @DisplayName("POST /guest: Returns 400 Bad Request when deviceId or deviceName exceeds 128 characters")
    void shouldRejectGuestLoginWithExcessiveDeviceFields() {
        String clientIp = "198.51.100.9";
        String longString = "x".repeat(129);

        // deviceId > 128 characters
        given()
                .header("X-Forwarded-For", clientIp)
                .contentType(ContentType.JSON)
                .body(Map.of("deviceId", longString))
                .when()
                .post(GUEST_AUTH_ENDPOINT)
                .then()
                .statusCode(400);

        // deviceName > 128 characters
        given()
                .header("X-Forwarded-For", clientIp)
                .contentType(ContentType.JSON)
                .body(Map.of("deviceName", longString))
                .when()
                .post(GUEST_AUTH_ENDPOINT)
                .then()
                .statusCode(400);
    }

    // =========================================================================
    // B) POST /api/v1/auth/guest/claim (Critical Flow)
    // =========================================================================

    @Test
    @DisplayName("POST /guest/claim: Returns 401 Unauthorized when request is unauthenticated")
    void shouldRejectClaimWithoutAuthentication() {
        // Arrange
        String clientIp = "198.51.100.12";
        Map<String, Object> claimPayload = Map.of(
                "email", "unauth_claim@ordago.dev",
                "password", "ValidSecurePassword123!"
        );

        // Act & Assert
        given()
                .header("X-Forwarded-For", clientIp)
                .contentType(ContentType.JSON)
                .body(claimPayload)
                .when()
                .post(GUEST_CLAIM_ENDPOINT)
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("POST /guest/claim: Returns 400 Bad Request (ACCOUNT_NOT_GUEST) when authenticated user is already regular")
    void shouldRejectClaimWhenAccountIsAlreadyRegular() {
        // Arrange: Register a regular user
        String clientIp = "198.51.100.13";
        String regularEmail = "already_regular_" + UUID.randomUUID() + "@ordago.dev";
        Map<String, Object> registerPayload = Map.of(
                "email", regularEmail,
                "password", "RegularMusPassword123!",
                "gameName", "RegPlayer",
                "tagLine", "REG1"
        );

        Response registerResponse = given()
                .header("X-Forwarded-For", clientIp)
                .contentType(ContentType.JSON)
                .body(registerPayload)
                .when()
                .post(REGISTER_ENDPOINT)
                .then()
                .statusCode(201)
                .extract().response();

        String regularAccessToken = registerResponse.path("accessToken");

        Map<String, Object> claimPayload = Map.of(
                "email", "new_regular_email@ordago.dev",
                "password", "AnotherPassword123!"
        );

        // Act & Assert: attempting to claim with a regular account fails with 400 ACCOUNT_NOT_GUEST
        given()
                .header("X-Forwarded-For", clientIp)
                .header("Authorization", "Bearer " + regularAccessToken)
                .contentType(ContentType.JSON)
                .body(claimPayload)
                .when()
                .post(GUEST_CLAIM_ENDPOINT)
                .then()
                .statusCode(400)
                .body("code", equalTo("ACCOUNT_NOT_GUEST"))
                .body("status", equalTo(400));
    }

    @Test
    @DisplayName("POST /guest/claim: Returns 409 Conflict (EMAIL_ALREADY_EXISTS) when claiming with an already registered email")
    void shouldRejectClaimWhenEmailAlreadyExists() {
        // Arrange: Create existing registered user A
        String clientIp = "198.51.100.14";
        String existingEmail = "existing_owner_" + UUID.randomUUID() + "@ordago.dev";
        given()
                .header("X-Forwarded-For", clientIp)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "email", existingEmail,
                        "password", "ExistingPassword123!",
                        "gameName", "ExistingOwner",
                        "tagLine", "OWN1"
                ))
                .when()
                .post(REGISTER_ENDPOINT)
                .then()
                .statusCode(201);

        // Create guest account B
        Response guestResponse = given()
                .header("X-Forwarded-For", clientIp)
                .contentType(ContentType.JSON)
                .body(Map.of("deviceId", "device-claim-conflict"))
                .when()
                .post(GUEST_AUTH_ENDPOINT)
                .then()
                .statusCode(201)
                .extract().response();

        String guestAccessToken = guestResponse.path("accessToken");

        // Act & Assert: Guest B attempts to claim with user A's email -> 409 Conflict
        given()
                .header("X-Forwarded-For", clientIp)
                .header("Authorization", "Bearer " + guestAccessToken)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "email", existingEmail,
                        "password", "BrandNewPassword123!"
                ))
                .when()
                .post(GUEST_CLAIM_ENDPOINT)
                .then()
                .statusCode(409)
                .body("code", equalTo("EMAIL_ALREADY_EXISTS"))
                .body("status", equalTo(409))
                .body("message", notNullValue());
    }

    @Test
    @DisplayName("POST /guest/claim: Returns 400 Bad Request when request payload contains invalid email or blank password")
    void shouldRejectClaimWithInvalidPayload() {
        // Arrange: Create guest account
        String clientIp = "198.51.100.15";
        Response guestResponse = given()
                .header("X-Forwarded-For", clientIp)
                .contentType(ContentType.JSON)
                .body("{}")
                .when()
                .post(GUEST_AUTH_ENDPOINT)
                .then()
                .statusCode(201)
                .extract().response();

        String guestAccessToken = guestResponse.path("accessToken");

        // Invalid email format
        given()
                .header("X-Forwarded-For", clientIp)
                .header("Authorization", "Bearer " + guestAccessToken)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "email", "not-a-valid-email",
                        "password", "ValidPassword123!"
                ))
                .when()
                .post(GUEST_CLAIM_ENDPOINT)
                .then()
                .statusCode(400);

        // Blank password
        given()
                .header("X-Forwarded-For", clientIp)
                .header("Authorization", "Bearer " + guestAccessToken)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "email", "valid_format@ordago.dev",
                        "password", "   "
                ))
                .when()
                .post(GUEST_CLAIM_ENDPOINT)
                .then()
                .statusCode(400);

        // Password too short (< 8 chars)
        given()
                .header("X-Forwarded-For", clientIp)
                .header("Authorization", "Bearer " + guestAccessToken)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "email", "valid_format@ordago.dev",
                        "password", "Short1!"
                ))
                .when()
                .post(GUEST_CLAIM_ENDPOINT)
                .then()
                .statusCode(400);

        // Password too long (> 128 chars)
        given()
                .header("X-Forwarded-For", clientIp)
                .header("Authorization", "Bearer " + guestAccessToken)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "email", "valid_format@ordago.dev",
                        "password", "A".repeat(129)
                ))
                .when()
                .post(GUEST_CLAIM_ENDPOINT)
                .then()
                .statusCode(400);

        // DeviceId too long (> 128 chars)
        given()
                .header("X-Forwarded-For", clientIp)
                .header("Authorization", "Bearer " + guestAccessToken)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "email", "valid_format@ordago.dev",
                        "password", "ValidSecurePassword123!",
                        "deviceId", "D".repeat(129)
                ))
                .when()
                .post(GUEST_CLAIM_ENDPOINT)
                .then()
                .statusCode(400);

        // DeviceName too long (> 128 chars)
        given()
                .header("X-Forwarded-For", clientIp)
                .header("Authorization", "Bearer " + guestAccessToken)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "email", "valid_format@ordago.dev",
                        "password", "ValidSecurePassword123!",
                        "deviceName", "N".repeat(129)
                ))
                .when()
                .post(GUEST_CLAIM_ENDPOINT)
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("POST /guest/claim: Returns 403 Forbidden (ACCOUNT_BANNED) when guest account is banned")
    void shouldRejectClaimWhenGuestAccountIsBanned() throws SQLException {
        // Arrange: Create guest account
        String clientIp = "198.51.100.17";
        Response guestResponse = given()
                .header("X-Forwarded-For", clientIp)
                .contentType(ContentType.JSON)
                .body("{}")
                .when()
                .post(GUEST_AUTH_ENDPOINT)
                .then()
                .statusCode(201)
                .extract().response();

        String guestAccessToken = guestResponse.path("accessToken");
        String guestUserId = guestResponse.path("user.id");

        // Ban the guest account directly in database
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE accounts SET is_banned = true, ban_reason = 'Cheating violation' WHERE id = ?")) {
            ps.setObject(1, UUID.fromString(guestUserId));
            ps.executeUpdate();
        }

        // Act & Assert
        given()
                .header("X-Forwarded-For", clientIp)
                .header("Authorization", "Bearer " + guestAccessToken)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "email", "banned_claim_" + UUID.randomUUID() + "@ordago.dev",
                        "password", "ValidSecurePassword123!"
                ))
                .when()
                .post(GUEST_CLAIM_ENDPOINT)
                .then()
                .statusCode(403)
                .body("code", equalTo("ACCOUNT_BANNED"))
                .body("status", equalTo(403));
    }

    @Test
    @DisplayName("POST /guest/claim: Valid claim converts guest to regular account with TOTAL profile preservation")
    void shouldClaimGuestAccountSuccessfullyPreservingProfile() {
        // Arrange 1: Create guest account and capture original profile details
        String clientIp = "198.51.100.16";
        Response guestResponse = given()
                .header("X-Forwarded-For", clientIp)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "deviceId", "device-preserve-test",
                        "deviceName", "Chrome Android"
                ))
                .when()
                .post(GUEST_AUTH_ENDPOINT)
                .then()
                .statusCode(201)
                .extract().response();

        String guestAccessToken = guestResponse.path("accessToken");
        String guestUserId = guestResponse.path("user.id");
        String originalGameName = guestResponse.path("user.gameName");
        String originalTagLine = guestResponse.path("user.tagLine");
        String originalPlayerTag = guestResponse.path("user.playerTag");
        int originalElo = guestResponse.path("user.eloRating");

        assertThat(originalGameName).matches("^Guest[0-9]{4}$");
        assertThat(originalTagLine).isEqualTo("666");
        assertThat(originalPlayerTag).isEqualTo(originalGameName + "#666");

        // Act: Claim guest account into regular account
        String claimedEmail = "claimed_user_" + UUID.randomUUID() + "@ordago.dev";
        String claimedPassword = "SecureClaimPassword123!";
        Map<String, Object> claimPayload = Map.of(
                "email", claimedEmail,
                "password", claimedPassword,
                "deviceId", "device-preserve-test-2",
                "deviceName", "Chrome Linux"
        );

        Response claimResponse = given()
                .header("X-Forwarded-For", clientIp)
                .header("Authorization", "Bearer " + guestAccessToken)
                .contentType(ContentType.JSON)
                .body(claimPayload)
                .when()
                .post(GUEST_CLAIM_ENDPOINT)
                .then()
                .statusCode(200)
                .body("accessToken", notNullValue())
                .body("refreshToken", notNullValue())
                .body("tokenType", equalTo("Bearer"))
                .body("user.id", equalTo(guestUserId))
                .body("user.email", equalTo(claimedEmail))
                .body("user.gameName", equalTo(originalGameName))
                .body("user.tagLine", equalTo(originalTagLine))
                .body("user.playerTag", equalTo(originalPlayerTag))
                .body("user.eloRating", equalTo(originalElo))
                .extract().response();

        // Assert 1: New JWT claims reflect regular account status while PRESERVING playerTag
        String claimedAccessToken = claimResponse.path("accessToken");
        assertThat(claimedAccessToken).isNotEqualTo(guestAccessToken);
        assertRegularJwtClaims(claimedAccessToken, originalPlayerTag, claimedEmail);

        // Assert 2: Authentication cookies were re-issued with the new tokens
        Cookie accessCookie = claimResponse.getDetailedCookie("access_token");
        assertThat(accessCookie).isNotNull();
        assertThat(accessCookie.getValue()).isEqualTo(claimedAccessToken);
        assertThat(accessCookie.isHttpOnly()).isTrue();

        Cookie refreshCookie = claimResponse.getDetailedCookie("refresh_token");
        assertThat(refreshCookie).isNotNull();
        assertThat(refreshCookie.getValue()).isEqualTo(claimResponse.path("refreshToken"));
        assertThat(refreshCookie.isHttpOnly()).isTrue();

        // Assert 3: GET /me with claimed token confirms total profile preservation
        given()
                .header("X-Forwarded-For", clientIp)
                .header("Authorization", "Bearer " + claimedAccessToken)
                .when()
                .get(ME_ENDPOINT)
                .then()
                .statusCode(200)
                .body("id", equalTo(guestUserId))
                .body("email", equalTo(claimedEmail))
                .body("gameName", equalTo(originalGameName))
                .body("tagLine", equalTo(originalTagLine))
                .body("playerTag", equalTo(originalPlayerTag))
                .body("eloRating", equalTo(originalElo));

        // Assert 4: Login with newly claimed credentials succeeds and preserves profile
        Response loginResponse = given()
                .header("X-Forwarded-For", clientIp)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "email", claimedEmail,
                        "password", claimedPassword
                ))
                .when()
                .post(LOGIN_ENDPOINT)
                .then()
                .statusCode(200)
                .body("user.id", equalTo(guestUserId))
                .body("user.email", equalTo(claimedEmail))
                .body("user.gameName", equalTo(originalGameName))
                .body("user.tagLine", equalTo(originalTagLine))
                .body("user.playerTag", equalTo(originalPlayerTag))
                .extract().response();

        String loginAccessToken = loginResponse.path("accessToken");
        assertRegularJwtClaims(loginAccessToken, originalPlayerTag, claimedEmail);

        // Assert 5: Attempting to claim AGAIN with the same account must fail (now regular)
        given()
                .header("X-Forwarded-For", clientIp)
                .header("Authorization", "Bearer " + claimedAccessToken)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "email", "second_attempt_" + UUID.randomUUID() + "@ordago.dev",
                        "password", "AnotherPassword123!"
                ))
                .when()
                .post(GUEST_CLAIM_ENDPOINT)
                .then()
                .statusCode(400)
                .body("code", equalTo("ACCOUNT_NOT_GUEST"));

        // Assert 6: Prior guest refresh token was revoked upon account claim
        String guestRefreshToken = guestResponse.path("refreshToken");
        given()
                .header("X-Forwarded-For", clientIp)
                .contentType(ContentType.JSON)
                .body(Map.of("refreshToken", guestRefreshToken))
                .when()
                .post("/api/v1/auth/refresh")
                .then()
                .statusCode(401)
                .body("code", equalTo("SESSION_REVOKED"));
    }
}
