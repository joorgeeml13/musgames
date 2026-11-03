package dev.ordago.auth.infrastructure.api;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import jakarta.inject.Inject;
import org.eclipse.microprofile.jwt.JsonWebToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.http.Cookie;
import io.restassured.response.Response;
import io.smallrye.jwt.auth.principal.DefaultJWTParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import dev.ordago.auth.infrastructure.persistence.AccountEntity;
import dev.ordago.auth.infrastructure.persistence.AccountPanacheRepository;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class AuthCookieIT {

    private static final String ACCESS_COOKIE_NAME = "access_token";
    private static final String REFRESH_COOKIE_NAME = "refresh_token";
    private static final String ACCESS_COOKIE_PATH = "/";
    private static final String REFRESH_COOKIE_PATH = "/api/v1/auth";

    @Inject
    AccountPanacheRepository accountPanacheRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DefaultJWTParser jwtParser = new DefaultJWTParser();

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

    private void assertEnrichedJwtClaims(String token, String expectedPlayerTag, boolean expectedEmailVerified) {
        assertThat(token).as("JWT token must not be null or blank").isNotBlank();

        // 1. Assert via Base64 JSON payload
        JsonNode payload = decodeJwtPayload(token);

        // Claim: playerTag
        assertThat(payload.hasNonNull("playerTag"))
                .as("JWT payload must contain non-null 'playerTag' claim").isTrue();
        assertThat(payload.get("playerTag").isTextual())
                .as("'playerTag' claim must be textual").isTrue();
        assertThat(payload.get("playerTag").asText())
                .as("'playerTag' claim must match expected player tag").isEqualTo(expectedPlayerTag);

        // Claim: emailVerified
        assertThat(payload.hasNonNull("emailVerified"))
                .as("JWT payload must contain non-null 'emailVerified' claim").isTrue();
        assertThat(payload.get("emailVerified").isBoolean())
                .as("'emailVerified' claim must be boolean").isTrue();
        assertThat(payload.get("emailVerified").asBoolean())
                .as("'emailVerified' claim must match expected boolean").isEqualTo(expectedEmailVerified);

        // Claim: jti
        assertThat(payload.hasNonNull("jti"))
                .as("JWT payload must contain non-null 'jti' claim").isTrue();
        assertThat(payload.get("jti").isTextual())
                .as("'jti' claim must be textual").isTrue();
        String jti = payload.get("jti").asText();
        assertThat(jti).as("'jti' claim must not be blank").isNotBlank();
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(
                () -> UUID.fromString(jti),
                "'jti' claim must be a valid UUID string: " + jti
        );

        // 2. Assert via MicroProfile JWT Parser
        try {
            JsonWebToken jwt = jwtParser.parseOnly(token);
            assertThat(jwt.getClaim("playerTag").toString().replace("\"", ""))
                    .as("MP-JWT playerTag claim").isEqualTo(expectedPlayerTag);
            assertThat(Boolean.parseBoolean(jwt.getClaim("emailVerified").toString()))
                    .as("MP-JWT emailVerified claim").isEqualTo(expectedEmailVerified);
            assertThat(jwt.getTokenID())
                    .as("MP-JWT token ID (jti)").isEqualTo(jti);
        } catch (Exception e) {
            throw new AssertionError("Failed to parse token with DefaultJWTParser", e);
        }
    }

    private String extractJti(String token) {
        return decodeJwtPayload(token).get("jti").asText();
    }

    private Map<String, Object> buildRegisterPayload(String prefix) {
        String unique = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return Map.of(
                "email", prefix + "_" + unique + "@ordago.dev",
                "password", "SecureMusPassword123!",
                "gameName", "Mus" + unique,
                "tagLine", "CK",
                "deviceId", "device-" + unique,
                "deviceName", "Linux Integration Runner"
        );
    }

    private Response register(Map<String, Object> payload) {
        return given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/api/v1/auth/register");
    }

    private Response login(String email, String password, String deviceId) {
        return given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "email", email,
                        "password", password,
                        "deviceId", deviceId,
                        "deviceName", "Linux Integration Runner"
                ))
                .when()
                .post("/api/v1/auth/login");
    }

    @Nested
    @DisplayName("1. Registration & Login Set-Cookie & Body Tokens")
    class RegistrationAndLoginCookies {

        @Test
        @DisplayName("Registration emits HttpOnly, SameSite=Strict cookies and tokens in response body")
        void shouldEmitCookiesAndBodyOnRegistration() {
            // Arrange
            Map<String, Object> payload = buildRegisterPayload("reg_cookie");

            // Act
            Response response = register(payload)
                    .then()
                    .statusCode(201)
                    .body("accessToken", notNullValue())
                    .body("refreshToken", notNullValue())
                    .body("tokenType", equalTo("Bearer"))
                    .body("expiresIn", notNullValue())
                    .extract().response();

            // Assert - Tokens in response body (mobile fallback)
            String bodyAccessToken = response.path("accessToken");
            String bodyRefreshToken = response.path("refreshToken");
            assertThat(bodyAccessToken).isNotBlank();
            assertThat(bodyRefreshToken).isNotBlank();

            // Assert - access_token cookie: HttpOnly; SameSite=Strict; Path=/; MaxAge > 0
            Cookie accessCookie = response.getDetailedCookie(ACCESS_COOKIE_NAME);
            assertThat(accessCookie).as("access_token cookie should be present").isNotNull();
            assertThat(accessCookie.getValue()).isEqualTo(bodyAccessToken);
            assertThat(accessCookie.isHttpOnly()).isTrue();
            assertThat(accessCookie.getPath()).isEqualTo(ACCESS_COOKIE_PATH);
            assertThat(accessCookie.getSameSite()).isEqualToIgnoringCase("Strict");
            assertThat(accessCookie.getMaxAge()).isPositive();

            // Assert - refresh_token cookie: HttpOnly; SameSite=Strict; Path=/api/v1/auth; MaxAge > 0
            Cookie refreshCookie = response.getDetailedCookie(REFRESH_COOKIE_NAME);
            assertThat(refreshCookie).as("refresh_token cookie should be present").isNotNull();
            assertThat(refreshCookie.getValue()).isEqualTo(bodyRefreshToken);
            assertThat(refreshCookie.isHttpOnly()).isTrue();
            assertThat(refreshCookie.getPath()).isEqualTo(REFRESH_COOKIE_PATH);
            assertThat(refreshCookie.getSameSite()).isEqualToIgnoringCase("Strict");
            assertThat(refreshCookie.getMaxAge()).isPositive();

            // Assert - Raw Set-Cookie headers verify attributes
            List<String> setCookieHeaders = response.getHeaders().getValues("Set-Cookie");
            assertThat(setCookieHeaders)
                    .anyMatch(h -> h.contains(ACCESS_COOKIE_NAME) && h.contains("HttpOnly") && h.contains("SameSite=Strict") && h.contains("Path=/"))
                    .anyMatch(h -> h.contains(REFRESH_COOKIE_NAME) && h.contains("HttpOnly") && h.contains("SameSite=Strict") && h.contains("Path=/api/v1/auth"));

            // Assert - Enriched claims (playerTag, emailVerified=false, valid jti UUID) in body and cookie
            String expectedPlayerTag = payload.get("gameName") + "#" + payload.get("tagLine");
            assertEnrichedJwtClaims(bodyAccessToken, expectedPlayerTag, false);
            assertEnrichedJwtClaims(accessCookie.getValue(), expectedPlayerTag, false);
        }

        @Test
        @DisplayName("Login emits HttpOnly, SameSite=Strict cookies and tokens in response body")
        void shouldEmitCookiesAndBodyOnLogin() {
            // Arrange
            Map<String, Object> payload = buildRegisterPayload("login_cookie");
            register(payload).then().statusCode(201);

            // Act
            Response response = login(
                    (String) payload.get("email"),
                    (String) payload.get("password"),
                    (String) payload.get("deviceId")
            ).then()
                    .statusCode(200)
                    .body("accessToken", notNullValue())
                    .body("refreshToken", notNullValue())
                    .body("tokenType", equalTo("Bearer"))
                    .extract().response();

            // Assert - Tokens in response body (mobile fallback)
            String bodyAccessToken = response.path("accessToken");
            String bodyRefreshToken = response.path("refreshToken");
            assertThat(bodyAccessToken).isNotBlank();
            assertThat(bodyRefreshToken).isNotBlank();

            // Assert - access_token cookie
            Cookie accessCookie = response.getDetailedCookie(ACCESS_COOKIE_NAME);
            assertThat(accessCookie).as("access_token cookie should be present").isNotNull();
            assertThat(accessCookie.getValue()).isEqualTo(bodyAccessToken);
            assertThat(accessCookie.isHttpOnly()).isTrue();
            assertThat(accessCookie.getPath()).isEqualTo(ACCESS_COOKIE_PATH);
            assertThat(accessCookie.getSameSite()).isEqualToIgnoringCase("Strict");
            assertThat(accessCookie.getMaxAge()).isPositive();

            // Assert - refresh_token cookie
            Cookie refreshCookie = response.getDetailedCookie(REFRESH_COOKIE_NAME);
            assertThat(refreshCookie).as("refresh_token cookie should be present").isNotNull();
            assertThat(refreshCookie.getValue()).isEqualTo(bodyRefreshToken);
            assertThat(refreshCookie.isHttpOnly()).isTrue();
            assertThat(refreshCookie.getPath()).isEqualTo(REFRESH_COOKIE_PATH);
            assertThat(refreshCookie.getSameSite()).isEqualToIgnoringCase("Strict");
            assertThat(refreshCookie.getMaxAge()).isPositive();

            // Assert - Enriched claims (playerTag, emailVerified=false, valid jti UUID) in body and cookie
            String expectedPlayerTag = payload.get("gameName") + "#" + payload.get("tagLine");
            assertEnrichedJwtClaims(bodyAccessToken, expectedPlayerTag, false);
            assertEnrichedJwtClaims(accessCookie.getValue(), expectedPlayerTag, false);
        }
    }

    @Nested
    @DisplayName("2. Authenticated /api/v1/auth/me Endpoint")
    class AuthenticatedMeEndpoint {

        @Test
        @DisplayName("Succeeds (200) when passing ONLY the access_token cookie (no Authorization header)")
        void shouldSucceedWithOnlyAccessTokenCookie() {
            // Arrange
            Map<String, Object> payload = buildRegisterPayload("me_cookie");
            Response regResponse = register(payload).then().statusCode(201).extract().response();
            String accessToken = regResponse.path("accessToken");
            String expectedEmail = (String) payload.get("email");
            String expectedGameName = (String) payload.get("gameName");

            // Act & Assert
            given()
                    .cookie(ACCESS_COOKIE_NAME, accessToken)
                    .when()
                    .get("/api/v1/auth/me")
                    .then()
                    .statusCode(200)
                    .body("email", equalTo(expectedEmail))
                    .body("gameName", equalTo(expectedGameName));
        }

        @Test
        @DisplayName("Succeeds (200) when passing ONLY the Authorization: Bearer header (no cookie, mobile fallback)")
        void shouldSucceedWithOnlyAuthorizationBearerHeader() {
            // Arrange
            Map<String, Object> payload = buildRegisterPayload("me_bearer");
            Response regResponse = register(payload).then().statusCode(201).extract().response();
            String accessToken = regResponse.path("accessToken");
            String expectedEmail = (String) payload.get("email");
            String expectedGameName = (String) payload.get("gameName");

            // Act & Assert
            given()
                    .header("Authorization", "Bearer " + accessToken)
                    .when()
                    .get("/api/v1/auth/me")
                    .then()
                    .statusCode(200)
                    .body("email", equalTo(expectedEmail))
                    .body("gameName", equalTo(expectedGameName));
        }

        @Test
        @DisplayName("Fails (401) when neither cookie nor Authorization header is provided")
        void shouldFailWhenNoCookieAndNoHeaderProvided() {
            // Act & Assert
            given()
                    .when()
                    .get("/api/v1/auth/me")
                    .then()
                    .statusCode(401);
        }

        @Test
        @DisplayName("Fails (401) when invalid access_token cookie is provided")
        void shouldFailWithInvalidAccessTokenCookie() {
            // Act & Assert
            given()
                    .cookie(ACCESS_COOKIE_NAME, "completely.invalid.jwt.token")
                    .when()
                    .get("/api/v1/auth/me")
                    .then()
                    .statusCode(401);
        }
    }

    @Nested
    @DisplayName("3. Token Refresh /api/v1/auth/refresh Endpoint")
    class RefreshEndpoint {

        @Test
        @DisplayName("Succeeds (200) when passing ONLY the refresh_token cookie with an empty body ({})")
        void shouldRefreshWithOnlyCookieAndEmptyJsonBody() {
            // Arrange
            Map<String, Object> payload = buildRegisterPayload("refresh_cookie_json");
            Response regResponse = register(payload).then().statusCode(201).extract().response();
            String originalRefreshToken = regResponse.path("refreshToken");
            String originalAccessToken = regResponse.path("accessToken");

            // Act
            Response response = given()
                    .cookie(REFRESH_COOKIE_NAME, originalRefreshToken)
                    .contentType(ContentType.JSON)
                    .body("{}")
                    .when()
                    .post("/api/v1/auth/refresh")
                    .then()
                    .statusCode(200)
                    .body("accessToken", notNullValue())
                    .body("refreshToken", notNullValue())
                    .extract().response();

            // Assert - Rotated tokens in body
            String newAccessToken = response.path("accessToken");
            String newRefreshToken = response.path("refreshToken");
            assertThat(newAccessToken).isNotBlank().isNotEqualTo(originalAccessToken);
            assertThat(newRefreshToken).isNotBlank().isNotEqualTo(originalRefreshToken);

            // Assert - Rotated Set-Cookie headers
            Cookie newAccessCookie = response.getDetailedCookie(ACCESS_COOKIE_NAME);
            assertThat(newAccessCookie).isNotNull();
            assertThat(newAccessCookie.getValue()).isEqualTo(newAccessToken);
            assertThat(newAccessCookie.isHttpOnly()).isTrue();
            assertThat(newAccessCookie.getPath()).isEqualTo(ACCESS_COOKIE_PATH);
            assertThat(newAccessCookie.getSameSite()).isEqualToIgnoringCase("Strict");
            assertThat(newAccessCookie.getMaxAge()).isPositive();

            Cookie newRefreshCookie = response.getDetailedCookie(REFRESH_COOKIE_NAME);
            assertThat(newRefreshCookie).isNotNull();
            assertThat(newRefreshCookie.getValue()).isEqualTo(newRefreshToken);
            assertThat(newRefreshCookie.isHttpOnly()).isTrue();
            assertThat(newRefreshCookie.getPath()).isEqualTo(REFRESH_COOKIE_PATH);
            assertThat(newRefreshCookie.getSameSite()).isEqualToIgnoringCase("Strict");
            assertThat(newRefreshCookie.getMaxAge()).isPositive();

            // Assert - Enriched claims (playerTag, emailVerified=false, valid jti UUID) in original and rotated tokens
            String expectedPlayerTag = payload.get("gameName") + "#" + payload.get("tagLine");
            assertEnrichedJwtClaims(originalAccessToken, expectedPlayerTag, false);
            assertEnrichedJwtClaims(newAccessToken, expectedPlayerTag, false);
            assertEnrichedJwtClaims(newAccessCookie.getValue(), expectedPlayerTag, false);
            assertThat(extractJti(newAccessToken))
                    .as("Rotated access token must have a distinct jti")
                    .isNotEqualTo(extractJti(originalAccessToken));
        }

        @Test
        @DisplayName("Succeeds (200) when passing refreshToken in JSON body (no cookie, mobile fallback)")
        void shouldRefreshWithRequestBodyMobileFallback() {
            // Arrange
            Map<String, Object> payload = buildRegisterPayload("refresh_mobile");
            Response regResponse = register(payload).then().statusCode(201).extract().response();
            String originalRefreshToken = regResponse.path("refreshToken");

            // Act
            Response response = given()
                    .contentType(ContentType.JSON)
                    .body(Map.of("refreshToken", originalRefreshToken))
                    .when()
                    .post("/api/v1/auth/refresh")
                    .then()
                    .statusCode(200)
                    .body("accessToken", notNullValue())
                    .body("refreshToken", notNullValue())
                    .extract().response();

            // Assert
            String newAccessToken = response.path("accessToken");
            String newRefreshToken = response.path("refreshToken");
            assertThat(newAccessToken).isNotBlank();
            assertThat(newRefreshToken).isNotBlank().isNotEqualTo(originalRefreshToken);

            // Assert - Enriched claims in body and cookie
            String expectedPlayerTag = payload.get("gameName") + "#" + payload.get("tagLine");
            assertEnrichedJwtClaims(newAccessToken, expectedPlayerTag, false);
            Cookie newAccessCookie = response.getDetailedCookie(ACCESS_COOKIE_NAME);
            assertThat(newAccessCookie).isNotNull();
            assertEnrichedJwtClaims(newAccessCookie.getValue(), expectedPlayerTag, false);
        }

        @Test
        @DisplayName("Fails (400) when neither cookie nor body contains a refresh token")
        void shouldFailRefreshWhenNeitherCookieNorBodyContainsToken() {
            // Act & Assert - Empty JSON body without cookie
            given()
                    .contentType(ContentType.JSON)
                    .body("{}")
                    .when()
                    .post("/api/v1/auth/refresh")
                    .then()
                    .statusCode(400);

            // Act & Assert - Blank refresh token in body
            given()
                    .contentType(ContentType.JSON)
                    .body(Map.of("refreshToken", "   "))
                    .when()
                    .post("/api/v1/auth/refresh")
                    .then()
                    .statusCode(400);
        }
    }

    @Nested
    @DisplayName("4. Logout /api/v1/auth/logout Endpoint")
    class LogoutEndpoint {

        @Test
        @DisplayName("Logout via cookie clears both access_token and refresh_token cookies (Max-Age=0)")
        void shouldClearCookiesOnLogoutUsingCookie() {
            // Arrange
            Map<String, Object> payload = buildRegisterPayload("logout_cookie");
            Response regResponse = register(payload).then().statusCode(201).extract().response();
            String refreshToken = regResponse.path("refreshToken");

            // Act
            Response response = given()
                    .cookie(REFRESH_COOKIE_NAME, refreshToken)
                    .contentType(ContentType.JSON)
                    .body("{}")
                    .when()
                    .post("/api/v1/auth/logout")
                    .then()
                    .statusCode(204)
                    .extract().response();

            // Assert - access_token cookie is cleared (Max-Age=0, Path=/)
            Cookie accessCookie = response.getDetailedCookie(ACCESS_COOKIE_NAME);
            assertThat(accessCookie).as("access_token cookie should be set for clearing").isNotNull();
            assertThat(accessCookie.getMaxAge()).isEqualTo(0);
            assertThat(accessCookie.getPath()).isEqualTo(ACCESS_COOKIE_PATH);
            assertThat(accessCookie.isHttpOnly()).isTrue();
            assertThat(accessCookie.getSameSite()).isEqualToIgnoringCase("Strict");
            assertThat(accessCookie.getValue()).isNullOrEmpty();

            // Assert - refresh_token cookie is cleared (Max-Age=0, Path=/api/v1/auth)
            Cookie refreshCookie = response.getDetailedCookie(REFRESH_COOKIE_NAME);
            assertThat(refreshCookie).as("refresh_token cookie should be set for clearing").isNotNull();
            assertThat(refreshCookie.getMaxAge()).isEqualTo(0);
            assertThat(refreshCookie.getPath()).isEqualTo(REFRESH_COOKIE_PATH);
            assertThat(refreshCookie.isHttpOnly()).isTrue();
            assertThat(refreshCookie.getSameSite()).isEqualToIgnoringCase("Strict");
            assertThat(refreshCookie.getValue()).isNullOrEmpty();

            // Assert - Set-Cookie headers contain Max-Age=0
            List<String> setCookieHeaders = response.getHeaders().getValues("Set-Cookie");
            assertThat(setCookieHeaders)
                    .anyMatch(h -> h.contains(ACCESS_COOKIE_NAME) && (h.contains("Max-Age=0") || h.contains("max-age=0")))
                    .anyMatch(h -> h.contains(REFRESH_COOKIE_NAME) && (h.contains("Max-Age=0") || h.contains("max-age=0")));

            // Assert - Invalidation verified: refresh with revoked token returns 401
            given()
                    .contentType(ContentType.JSON)
                    .body(Map.of("refreshToken", refreshToken))
                    .when()
                    .post("/api/v1/auth/refresh")
                    .then()
                    .statusCode(401);
        }

        @Test
        @DisplayName("Logout via JSON body clears both cookies (Max-Age=0, mobile fallback)")
        void shouldClearCookiesOnLogoutUsingBodyMobileFallback() {
            // Arrange
            Map<String, Object> payload = buildRegisterPayload("logout_body");
            Response regResponse = register(payload).then().statusCode(201).extract().response();
            String refreshToken = regResponse.path("refreshToken");

            // Act
            Response response = given()
                    .contentType(ContentType.JSON)
                    .body(Map.of("refreshToken", refreshToken))
                    .when()
                    .post("/api/v1/auth/logout")
                    .then()
                    .statusCode(204)
                    .extract().response();

            // Assert - Cookies cleared
            Cookie accessCookie = response.getDetailedCookie(ACCESS_COOKIE_NAME);
            assertThat(accessCookie).isNotNull();
            assertThat(accessCookie.getMaxAge()).isEqualTo(0);

            Cookie refreshCookie = response.getDetailedCookie(REFRESH_COOKIE_NAME);
            assertThat(refreshCookie).isNotNull();
            assertThat(refreshCookie.getMaxAge()).isEqualTo(0);

            // Assert - Token is revoked in database
            given()
                    .contentType(ContentType.JSON)
                    .body(Map.of("refreshToken", refreshToken))
                    .when()
                    .post("/api/v1/auth/refresh")
                    .then()
                    .statusCode(401);
        }
    }

    @Nested
    @DisplayName("5. Change Password /api/v1/auth/change-password Endpoint")
    class ChangePasswordEndpoint {

        @Test
        @DisplayName("Change password clears both access_token and refresh_token cookies (Max-Age=0)")
        void shouldClearCookiesOnChangePassword() {
            // Arrange
            Map<String, Object> payload = buildRegisterPayload("changepw_cookie");
            Response regResponse = register(payload).then().statusCode(201).extract().response();
            String accessToken = regResponse.path("accessToken");
            String refreshToken = regResponse.path("refreshToken");
            String currentPassword = (String) payload.get("password");

            // Act
            Response response = given()
                    .cookie(ACCESS_COOKIE_NAME, accessToken)
                    .contentType(ContentType.JSON)
                    .body(Map.of(
                            "currentPassword", currentPassword,
                            "newPassword", "BrandNewPassword123!"
                    ))
                    .when()
                    .post("/api/v1/auth/change-password")
                    .then()
                    .statusCode(204)
                    .extract().response();

            // Assert - access_token cookie is cleared (Max-Age=0, Path=/)
            Cookie accessCookie = response.getDetailedCookie(ACCESS_COOKIE_NAME);
            assertThat(accessCookie).as("access_token cookie should be set for clearing").isNotNull();
            assertThat(accessCookie.getMaxAge()).isEqualTo(0);
            assertThat(accessCookie.getPath()).isEqualTo(ACCESS_COOKIE_PATH);
            assertThat(accessCookie.isHttpOnly()).isTrue();
            assertThat(accessCookie.getSameSite()).isEqualToIgnoringCase("Strict");

            // Assert - refresh_token cookie is cleared (Max-Age=0, Path=/api/v1/auth)
            Cookie refreshCookie = response.getDetailedCookie(REFRESH_COOKIE_NAME);
            assertThat(refreshCookie).as("refresh_token cookie should be set for clearing").isNotNull();
            assertThat(refreshCookie.getMaxAge()).isEqualTo(0);
            assertThat(refreshCookie.getPath()).isEqualTo(REFRESH_COOKIE_PATH);
            assertThat(refreshCookie.isHttpOnly()).isTrue();
            assertThat(refreshCookie.getSameSite()).isEqualToIgnoringCase("Strict");

            // Assert - Old refresh token is revoked in database
            given()
                    .contentType(ContentType.JSON)
                    .body(Map.of("refreshToken", refreshToken))
                    .when()
                    .post("/api/v1/auth/refresh")
                    .then()
                    .statusCode(401);
        }
    }

    @Nested
    @DisplayName("6. Enriched JWT Access Token Claims (/register, /login, /refresh, emailVerified, jti)")
    class EnrichedJwtClaimsTests {

        @Test
        @DisplayName("Registration issues JWT in body and cookie with playerTag, emailVerified=false, and valid jti UUID")
        void shouldContainEnrichedClaimsOnRegistrationInBothBodyAndCookie() {
            // Arrange
            Map<String, Object> payload = buildRegisterPayload("claim_reg");
            String expectedPlayerTag = payload.get("gameName") + "#" + payload.get("tagLine");

            // Act
            Response response = register(payload).then().statusCode(201).extract().response();

            // Assert
            String bodyAccessToken = response.path("accessToken");
            Cookie accessCookie = response.getDetailedCookie(ACCESS_COOKIE_NAME);
            assertThat(accessCookie).isNotNull();

            // Both body and cookie contain enriched claims
            assertEnrichedJwtClaims(bodyAccessToken, expectedPlayerTag, false);
            assertEnrichedJwtClaims(accessCookie.getValue(), expectedPlayerTag, false);

            // Cookie and body tokens match identically
            assertThat(accessCookie.getValue()).isEqualTo(bodyAccessToken);
        }

        @Test
        @DisplayName("Login issues JWT in body and cookie with playerTag, emailVerified=false, and distinct jti UUID")
        void shouldContainEnrichedClaimsOnLoginInBothBodyAndCookie() {
            // Arrange
            Map<String, Object> payload = buildRegisterPayload("claim_login");
            String expectedPlayerTag = payload.get("gameName") + "#" + payload.get("tagLine");
            Response regResponse = register(payload).then().statusCode(201).extract().response();
            String regAccessToken = regResponse.path("accessToken");
            String regJti = extractJti(regAccessToken);

            // Act
            Response response = login(
                    (String) payload.get("email"),
                    (String) payload.get("password"),
                    (String) payload.get("deviceId")
            ).then().statusCode(200).extract().response();

            // Assert
            String loginAccessToken = response.path("accessToken");
            Cookie accessCookie = response.getDetailedCookie(ACCESS_COOKIE_NAME);
            assertThat(accessCookie).isNotNull();

            // Both body and cookie contain enriched claims
            assertEnrichedJwtClaims(loginAccessToken, expectedPlayerTag, false);
            assertEnrichedJwtClaims(accessCookie.getValue(), expectedPlayerTag, false);

            // Token has a newly generated, distinct jti UUID
            String loginJti = extractJti(loginAccessToken);
            assertThat(loginJti).isNotEqualTo(regJti);
        }

        @Test
        @DisplayName("Refresh issues rotated JWT in body and cookie with playerTag, emailVerified=false, and new jti UUID")
        void shouldContainEnrichedClaimsOnRefreshInBothBodyAndCookie() {
            // Arrange
            Map<String, Object> payload = buildRegisterPayload("claim_refresh");
            String expectedPlayerTag = payload.get("gameName") + "#" + payload.get("tagLine");
            Response regResponse = register(payload).then().statusCode(201).extract().response();
            String originalRefreshToken = regResponse.path("refreshToken");
            String originalAccessToken = regResponse.path("accessToken");
            String originalJti = extractJti(originalAccessToken);

            // Act
            Response response = given()
                    .cookie(REFRESH_COOKIE_NAME, originalRefreshToken)
                    .contentType(ContentType.JSON)
                    .body("{}")
                    .when()
                    .post("/api/v1/auth/refresh")
                    .then()
                    .statusCode(200)
                    .extract().response();

            // Assert
            String refreshedAccessToken = response.path("accessToken");
            Cookie accessCookie = response.getDetailedCookie(ACCESS_COOKIE_NAME);
            assertThat(accessCookie).isNotNull();

            // Both body and cookie contain enriched claims
            assertEnrichedJwtClaims(refreshedAccessToken, expectedPlayerTag, false);
            assertEnrichedJwtClaims(accessCookie.getValue(), expectedPlayerTag, false);

            // Distinct jti from original access token
            String refreshedJti = extractJti(refreshedAccessToken);
            assertThat(refreshedJti).isNotEqualTo(originalJti);
        }

        @Test
        @DisplayName("Email verification transitions emailVerified claim from false to true in subsequent login and refresh tokens")
        void shouldTransitionEmailVerifiedClaimFromFalseToTrueAfterVerification() {
            // Arrange: 1. Register unverified account
            Map<String, Object> payload = buildRegisterPayload("claim_verify");
            String email = (String) payload.get("email");
            String expectedPlayerTag = payload.get("gameName") + "#" + payload.get("tagLine");

            Response regResponse = register(payload).then().statusCode(201).extract().response();
            String regAccessToken = regResponse.path("accessToken");
            Cookie regAccessCookie = regResponse.getDetailedCookie(ACCESS_COOKIE_NAME);
            assertEnrichedJwtClaims(regAccessToken, expectedPlayerTag, false);
            assertEnrichedJwtClaims(regAccessCookie.getValue(), expectedPlayerTag, false);

            // 2. Fetch email verification token directly from repository
            AccountEntity account = accountPanacheRepository.findByEmail(email).orElseThrow();
            String verificationToken = account.emailVerificationToken;
            assertThat(verificationToken).isNotBlank();

            // Act: 3. Verify email via REST endpoint
            given()
                    .contentType(ContentType.JSON)
                    .body(Map.of("token", verificationToken))
                    .when()
                    .post("/api/v1/auth/verify-email")
                    .then()
                    .statusCode(204);

            // Verify database state is updated
            accountPanacheRepository.getEntityManager().clear();
            AccountEntity updatedAccount = accountPanacheRepository.findByEmail(email).orElseThrow();
            assertThat(updatedAccount.emailVerified).isTrue();

            // Act & Assert: 4. Subsequent Login issues JWT with emailVerified=true in body & cookie
            Response loginResponse = login(
                    email,
                    (String) payload.get("password"),
                    (String) payload.get("deviceId")
            ).then().statusCode(200).extract().response();

            String loginAccessToken = loginResponse.path("accessToken");
            String loginRefreshToken = loginResponse.path("refreshToken");
            Cookie loginAccessCookie = loginResponse.getDetailedCookie(ACCESS_COOKIE_NAME);

            assertEnrichedJwtClaims(loginAccessToken, expectedPlayerTag, true);
            assertEnrichedJwtClaims(loginAccessCookie.getValue(), expectedPlayerTag, true);

            // Act & Assert: 5. Subsequent Refresh issues rotated JWT with emailVerified=true in body & cookie
            Response refreshResponse = given()
                    .cookie(REFRESH_COOKIE_NAME, loginRefreshToken)
                    .contentType(ContentType.JSON)
                    .body("{}")
                    .when()
                    .post("/api/v1/auth/refresh")
                    .then()
                    .statusCode(200)
                    .extract().response();

            String refreshAccessToken = refreshResponse.path("accessToken");
            Cookie refreshAccessCookie = refreshResponse.getDetailedCookie(ACCESS_COOKIE_NAME);

            assertEnrichedJwtClaims(refreshAccessToken, expectedPlayerTag, true);
            assertEnrichedJwtClaims(refreshAccessCookie.getValue(), expectedPlayerTag, true);

            // jti is unique across each token issuance
            String regJti = extractJti(regAccessToken);
            String loginJti = extractJti(loginAccessToken);
            String refreshJti = extractJti(refreshAccessToken);

            assertThat(Set.of(regJti, loginJti, refreshJti))
                    .as("Each issued access token must possess a unique, distinct jti")
                    .hasSize(3);
        }

        @Test
        @DisplayName("Every issued access token generates a unique, non-repeated jti UUID")
        void shouldGenerateUniqueJtiForEachIssuedAccessToken() {
            // Arrange
            Map<String, Object> payload = buildRegisterPayload("claim_jti");
            String email = (String) payload.get("email");
            String password = (String) payload.get("password");
            String deviceId = (String) payload.get("deviceId");

            // 1. Register
            Response regResponse = register(payload).then().statusCode(201).extract().response();
            String token1 = regResponse.path("accessToken");
            String refreshToken = regResponse.path("refreshToken");

            // 2. Login
            Response loginResponse = login(email, password, deviceId).then().statusCode(200).extract().response();
            String token2 = loginResponse.path("accessToken");

            // 3. Refresh
            Response refreshResponse = given()
                    .cookie(REFRESH_COOKIE_NAME, refreshToken)
                    .contentType(ContentType.JSON)
                    .body("{}")
                    .when()
                    .post("/api/v1/auth/refresh")
                    .then()
                    .statusCode(200)
                    .extract().response();
            String token3 = refreshResponse.path("accessToken");

            // Assert - all jti claims are distinct UUIDs
            String jti1 = extractJti(token1);
            String jti2 = extractJti(token2);
            String jti3 = extractJti(token3);

            assertThat(Set.of(jti1, jti2, jti3))
                    .as("All issued access tokens must have distinct jti UUIDs")
                    .hasSize(3);
        }
    }
}
