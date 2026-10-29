package dev.ordago.profile.infrastructure.api;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import java.util.Map;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserProfileResourceIT {

    private static String accessToken;
    private static boolean initialized = false;

    @BeforeEach
    void setUpUser() {
        if (initialized) {
            return;
        }

        Map<String, Object> registerPayload = Map.of(
                "email", "profile_test@ordago.dev",
                "password", "SecurePassword123!",
                "gameName", "ProfilePro",
                "tagLine", "PRO1",
                "deviceId", "device-test-profile"
        );

        Response response = given()
                .contentType(ContentType.JSON)
                .body(registerPayload)
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(201)
                .extract().response();

        accessToken = response.path("accessToken");
        initialized = true;
    }

    @Test
    @Order(1)
    void shouldGetPublicProfileByRiotId() {
        given()
                .when()
                .get("/api/v1/profiles/ProfilePro/PRO1")
                .then()
                .statusCode(200)
                .body("gameName", equalTo("ProfilePro"))
                .body("tagLine", equalTo("PRO1"))
                .body("riotId", equalTo("ProfilePro#PRO1"))
                .body("eloRating", equalTo(1200))
                .body("id", nullValue())
                .body("soundEffectsVolume", nullValue());
    }

    @Test
    @Order(2)
    void shouldGetPublicProfileByRiotIdCaseInsensitive() {
        given()
                .when()
                .get("/api/v1/profiles/profilepro/pro1")
                .then()
                .statusCode(200)
                .body("gameName", equalTo("ProfilePro"))
                .body("tagLine", equalTo("PRO1"))
                .body("riotId", equalTo("ProfilePro#PRO1"));
    }

    @Test
    @Order(3)
    void shouldReturn404ForNonexistentPublicProfile() {
        given()
                .when()
                .get("/api/v1/profiles/NoSuchPlayer/404")
                .then()
                .statusCode(404);
    }

    @Test
    @Order(4)
    void shouldReturn400ForInvalidPublicProfileRiotId() {
        // gameName too short (< 2 characters)
        given()
                .when()
                .get("/api/v1/profiles/A/PRO1")
                .then()
                .statusCode(400);

        // tagLine too long (> 6 characters)
        given()
                .when()
                .get("/api/v1/profiles/ProfilePro/WAYTOOLONG")
                .then()
                .statusCode(400);
    }

    @Test
    @Order(5)
    void shouldGetMyProfileWhenAuthenticated() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get("/api/v1/profiles/me")
                .then()
                .statusCode(200)
                .body("gameName", equalTo("ProfilePro"))
                .body("tagLine", equalTo("PRO1"))
                .body("activeTitle", equalTo("Novato del Chinchón"));
    }

    @Test
    @Order(6)
    void shouldRejectGetMyProfileWithoutAuthentication() {
        given()
                .when()
                .get("/api/v1/profiles/me")
                .then()
                .statusCode(401);
    }

    @Test
    @Order(7)
    void shouldUpdateMyProfileWhenAuthenticated() {
        Map<String, Object> updatePayload = Map.of(
                "displayName", "Pro Player",
                "bio", "Playing Mus since 2010.",
                "soundEffectsVolume", 95,
                "allowDirectChallenges", false
        );

        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(updatePayload)
                .when()
                .put("/api/v1/profiles/me")
                .then()
                .statusCode(200)
                .body("displayName", equalTo("Pro Player"))
                .body("bio", equalTo("Playing Mus since 2010."))
                .body("soundEffectsVolume", equalTo(95))
                .body("allowDirectChallenges", equalTo(false));
    }

    @Test
    @Order(8)
    void shouldRejectProfileUpdateWithInvalidVolume() {
        Map<String, Object> invalidPayload = Map.of(
                "soundEffectsVolume", 150
        );

        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(invalidPayload)
                .when()
                .put("/api/v1/profiles/me")
                .then()
                .statusCode(400);
    }
}
