package dev.ordago.auth.infrastructure.api;

import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import jakarta.inject.Inject;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.smallrye.jwt.auth.principal.DefaultJWTParser;
import dev.ordago.auth.infrastructure.persistence.AccountEntity;
import dev.ordago.auth.infrastructure.persistence.AccountPanacheRepository;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthResourceIT {

    @Inject
    AccountPanacheRepository accountPanacheRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DefaultJWTParser jwtParser = new DefaultJWTParser();

    private static String accessToken;
    private static String refreshToken;
    private static String previousRefreshToken;

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
        JsonNode payload = decodeJwtPayload(token);

        assertThat(payload.hasNonNull("playerTag"))
                .as("JWT payload must contain non-null 'playerTag' claim").isTrue();
        assertThat(payload.get("playerTag").isTextual())
                .as("'playerTag' claim must be textual").isTrue();
        assertThat(payload.get("playerTag").asText())
                .as("'playerTag' claim must match expected player tag").isEqualTo(expectedPlayerTag);

        assertThat(payload.hasNonNull("emailVerified"))
                .as("JWT payload must contain non-null 'emailVerified' claim").isTrue();
        assertThat(payload.get("emailVerified").isBoolean())
                .as("'emailVerified' claim must be boolean").isTrue();
        assertThat(payload.get("emailVerified").asBoolean())
                .as("'emailVerified' claim must match expected boolean").isEqualTo(expectedEmailVerified);

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

    @Test
    @Order(1)
    void shouldRegisterNewUserSuccessfully() {
        Map<String, Object> registerPayload = Map.of(
                "email", "it_player@ordago.dev",
                "password", "SecureMusPassword123!",
                "gameName", "ITMusMaster",
                "tagLine", "ESP",
                "deviceId", "device-it-1",
                "deviceName", "Linux Dev"
        );

        Response response = given()
                .contentType(ContentType.JSON)
                .body(registerPayload)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(201)
                .body("accessToken", notNullValue())
                .body("refreshToken", notNullValue())
                .body("tokenType", equalTo("Bearer"))
                .body("user.email", equalTo("it_player@ordago.dev"))
                .body("user.gameName", equalTo("ITMusMaster"))
                .body("user.tagLine", equalTo("ESP"))
                .body("user.eloRating", equalTo(1200))
                .extract().response();

        accessToken = response.path("accessToken");
        refreshToken = response.path("refreshToken");
        assertEnrichedJwtClaims(accessToken, "ITMusMaster#ESP", false);
    }

    @Test
    @Order(2)
    void shouldRejectDuplicateRegistrationWithSameEmail() {
        Map<String, Object> duplicatePayload = Map.of(
                "email", "it_player@ordago.dev",
                "password", "AnotherPassword123!",
                "gameName", "ITMusMaster2",
                "tagLine", "ESP"
        );

        given()
                .contentType(ContentType.JSON)
                .body(duplicatePayload)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(409);
    }

    @Test
    @Order(3)
    void shouldRejectRegistrationWithDuplicatePlayerTagSameCase() {
        Map<String, Object> duplicatePayload = Map.of(
                "email", "it_dup_tag1@ordago.dev",
                "password", "AnotherPassword123!",
                "gameName", "ITMusMaster",
                "tagLine", "ESP"
        );

        given()
                .contentType(ContentType.JSON)
                .body(duplicatePayload)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(409);
    }

    @Test
    @Order(4)
    void shouldRejectRegistrationWithDuplicatePlayerTagDifferentCase() {
        Map<String, Object> duplicatePayload = Map.of(
                "email", "it_dup_tag2@ordago.dev",
                "password", "AnotherPassword123!",
                "gameName", "itmusmaster",
                "tagLine", "esp"
        );

        given()
                .contentType(ContentType.JSON)
                .body(duplicatePayload)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(409);
    }

    @Test
    @Order(5)
    void shouldRejectRegistrationWithInvalidGameName() {
        Map<String, Object> invalidPayload = Map.of(
                "email", "invalid_game_name@ordago.dev",
                "password", "Password123!",
                "gameName", "A",
                "tagLine", "ESP"
        );

        given()
                .contentType(ContentType.JSON)
                .body(invalidPayload)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(400);
    }

    @Test
    @Order(6)
    void shouldRejectRegistrationWithInvalidTagLine() {
        Map<String, Object> invalidPayload = Map.of(
                "email", "invalid_tag_line@ordago.dev",
                "password", "Password123!",
                "gameName", "ValidGameName",
                "tagLine", "TOOLONGTAG"
        );

        given()
                .contentType(ContentType.JSON)
                .body(invalidPayload)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(400);
    }

    @Test
    @Order(7)
    void shouldLoginWithValidCredentials() {
        Map<String, Object> loginPayload = Map.of(
                "email", "it_player@ordago.dev",
                "password", "SecureMusPassword123!"
        );

        Response response = given()
                .contentType(ContentType.JSON)
                .body(loginPayload)
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(200)
                .body("accessToken", notNullValue())
                .body("refreshToken", notNullValue())
                .body("user.email", equalTo("it_player@ordago.dev"))
                .extract().response();

        accessToken = response.path("accessToken");
        refreshToken = response.path("refreshToken");
        assertEnrichedJwtClaims(accessToken, "ITMusMaster#ESP", false);
    }

    @Test
    @Order(8)
    void shouldRejectLoginWithInvalidPassword() {
        Map<String, Object> invalidPayload = Map.of(
                "email", "it_player@ordago.dev",
                "password", "CompletelyWrongPassword!"
        );

        given()
                .contentType(ContentType.JSON)
                .body(invalidPayload)
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(401);
    }

    @Test
    @Order(9)
    void shouldGetMeWhenAuthenticated() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get("/api/v1/auth/me")
                .then()
                .statusCode(200)
                .body("email", equalTo("it_player@ordago.dev"))
                .body("gameName", equalTo("ITMusMaster"))
                .body("eloRating", equalTo(1200));
    }

    @Test
    @Order(10)
    void shouldRejectGetMeWithoutAuthentication() {
        given()
                .when()
                .get("/api/v1/auth/me")
                .then()
                .statusCode(401);
    }

    @Test
    @Order(11)
    void shouldRotateRefreshToken() {
        previousRefreshToken = refreshToken;

        Map<String, Object> refreshPayload = Map.of(
                "refreshToken", refreshToken
        );

        Response response = given()
                .contentType(ContentType.JSON)
                .body(refreshPayload)
                .when()
                .post("/api/v1/auth/refresh")
                .then()
                .statusCode(200)
                .body("accessToken", notNullValue())
                .body("refreshToken", notNullValue())
                .extract().response();

        accessToken = response.path("accessToken");
        refreshToken = response.path("refreshToken");
        assertEnrichedJwtClaims(accessToken, "ITMusMaster#ESP", false);
    }

    @Test
    @Order(12)
    void shouldAllowRefreshTokenGraceWindowRetry() {
        // Present previousRefreshToken immediately (simulating network retry within 30s grace window)
        Map<String, Object> retryPayload = Map.of(
                "refreshToken", previousRefreshToken
        );

        Response response = given()
                .contentType(ContentType.JSON)
                .body(retryPayload)
                .when()
                .post("/api/v1/auth/refresh")
                .then()
                .statusCode(200)
                .body("accessToken", notNullValue())
                .body("refreshToken", notNullValue())
                .extract().response();

        accessToken = response.path("accessToken");
        refreshToken = response.path("refreshToken");
        assertEnrichedJwtClaims(accessToken, "ITMusMaster#ESP", false);
    }

    @Test
    @Order(13)
    void shouldLogoutSuccessfully() {
        Map<String, Object> logoutPayload = Map.of(
                "refreshToken", refreshToken
        );

        given()
                .contentType(ContentType.JSON)
                .body(logoutPayload)
                .when()
                .post("/api/v1/auth/logout")
                .then()
                .statusCode(204);

        // After logout, refreshing with the revoked token should fail with 401
        given()
                .contentType(ContentType.JSON)
                .body(logoutPayload)
                .when()
                .post("/api/v1/auth/refresh")
                .then()
                .statusCode(401);
    }

    @Test
    @Order(14)
    void shouldRejectEmailVerificationWithInvalidToken() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("token", "completely-invalid-verification-token"))
                .when()
                .post("/api/v1/auth/verify-email")
                .then()
                .statusCode(400);
    }

    @Test
    @Order(15)
    void shouldVerifyEmailSuccessfully() {
        AccountEntity account = accountPanacheRepository.findByEmail("it_player@ordago.dev").orElseThrow();
        String token = account.emailVerificationToken;
        assertThat(token).isNotBlank();

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("token", token))
                .when()
                .post("/api/v1/auth/verify-email")
                .then()
                .statusCode(204);

        accountPanacheRepository.getEntityManager().clear();
        AccountEntity updated = accountPanacheRepository.findByEmail("it_player@ordago.dev").orElseThrow();
        assertThat(updated.emailVerified).isTrue();
        assertThat(updated.emailVerificationToken).isNull();
    }

    @Test
    @Order(16)
    void shouldHandleResendVerificationRequest() {
        // Unverified user / verified user / non-existent user should all return 204
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("email", "it_player@ordago.dev"))
                .when()
                .post("/api/v1/auth/resend-verification")
                .then()
                .statusCode(204);

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("email", "nonexistent@ordago.dev"))
                .when()
                .post("/api/v1/auth/resend-verification")
                .then()
                .statusCode(204);
    }

    @Test
    @Order(17)
    void shouldHandleForgotPasswordRequest() {
        // Non-existent email returns 204 (anti-enumeration)
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("email", "nobody@ordago.dev"))
                .when()
                .post("/api/v1/auth/forgot-password")
                .then()
                .statusCode(204);

        // Real user generates reset token
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("email", "it_player@ordago.dev"))
                .when()
                .post("/api/v1/auth/forgot-password")
                .then()
                .statusCode(204);

        AccountEntity account = accountPanacheRepository.findByEmail("it_player@ordago.dev").orElseThrow();
        assertThat(account.passwordResetToken).isNotBlank();
        assertThat(account.passwordResetExpiresAt).isNotNull();
    }

    @Test
    @Order(18)
    void shouldRejectResetPasswordWithInvalidToken() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "token", "invalid-reset-token",
                        "newPassword", "NewValidPassword123!"
                ))
                .when()
                .post("/api/v1/auth/reset-password")
                .then()
                .statusCode(400);
    }

    @Test
    @Order(19)
    void shouldResetPasswordSuccessfully() {
        AccountEntity account = accountPanacheRepository.findByEmail("it_player@ordago.dev").orElseThrow();
        String resetToken = account.passwordResetToken;

        given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "token", resetToken,
                        "newPassword", "ResetSuccessfulPassword123!"
                ))
                .when()
                .post("/api/v1/auth/reset-password")
                .then()
                .statusCode(204);

        // Login with old password fails
        given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "email", "it_player@ordago.dev",
                        "password", "SecureMusPassword123!"
                ))
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(401);

        // Login with new password succeeds and refreshes accessToken
        Response response = given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "email", "it_player@ordago.dev",
                        "password", "ResetSuccessfulPassword123!"
                ))
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(200)
                .extract().response();

        accessToken = response.path("accessToken");
        assertThat(accessToken).isNotBlank();
        assertEnrichedJwtClaims(accessToken, "ITMusMaster#ESP", true);
    }

    @Test
    @Order(20)
    void shouldRejectChangePasswordWithWrongCurrentPassword() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "currentPassword", "CompletelyWrongOldPassword!",
                        "newPassword", "YetAnotherPassword123!"
                ))
                .when()
                .post("/api/v1/auth/change-password")
                .then()
                .statusCode(401);
    }

    @Test
    @Order(21)
    void shouldChangePasswordSuccessfully() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "currentPassword", "ResetSuccessfulPassword123!",
                        "newPassword", "FinalChangedPassword123!"
                ))
                .when()
                .post("/api/v1/auth/change-password")
                .then()
                .statusCode(204);

        // Login with final password succeeds
        given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "email", "it_player@ordago.dev",
                        "password", "FinalChangedPassword123!"
                ))
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(200);
    }
}
