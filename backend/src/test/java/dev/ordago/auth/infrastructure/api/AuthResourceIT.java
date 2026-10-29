package dev.ordago.auth.infrastructure.api;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import java.util.Map;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthResourceIT {

    private static String accessToken;
    private static String refreshToken;
    private static String previousRefreshToken;

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
    void shouldRejectRegistrationWithDuplicateRiotIdSameCase() {
        Map<String, Object> duplicatePayload = Map.of(
                "email", "it_dup_riot1@ordago.dev",
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
    void shouldRejectRegistrationWithDuplicateRiotIdDifferentCase() {
        Map<String, Object> duplicatePayload = Map.of(
                "email", "it_dup_riot2@ordago.dev",
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
}
