package org.mus.player.infrastructure.adapter.input.rest;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.mus.player.application.dto.CreatePlayerCommand;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;

@QuarkusTest
class LeaderboardResourceTest {

    @Test
    void testGetLeaderboard() {
        given()
          .when().get("/api/leaderboard")
          .then()
             .statusCode(200)
             .body("success", is(true))
             .body("data.size()", greaterThanOrEqualTo(1))
             .body("message", equalTo("Clasificación obtenida con éxito"));
    }

    @Test
    void testGetLeaderboardEnglish() {
        given()
          .header("Accept-Language", "en")
          .when().get("/api/leaderboard")
          .then()
             .statusCode(200)
             .body("success", is(true))
             .body("data.size()", greaterThanOrEqualTo(1))
             .body("message", equalTo("Leaderboard retrieved successfully"));
    }

    @Test
    void testGetLeaderboardPaginationLimit() {
        given()
          .queryParam("limit", 2)
          .when().get("/api/leaderboard")
          .then()
             .statusCode(200)
             .body("success", is(true))
             .body("data", hasSize(2));
    }

    @Test
    void testGetLeaderboardZeroOrNegativeLimitClampedToOne() {
        given()
          .queryParam("limit", 0)
          .when().get("/api/leaderboard")
          .then()
             .statusCode(200)
             .body("data", hasSize(1));

        given()
          .queryParam("limit", -5)
          .when().get("/api/leaderboard")
          .then()
             .statusCode(200)
             .body("data", hasSize(1));
    }

    @Test
    void testGetLeaderboardHighLimitClampedToMax() {
        given()
          .queryParam("limit", 500)
          .when().get("/api/leaderboard")
          .then()
             .statusCode(200)
             .body("data.size()", greaterThanOrEqualTo(1));
    }

    @Test
    void testGetLeaderboardInvalidLimitParam() {
        given()
          .queryParam("limit", "not_a_number")
          .when().get("/api/leaderboard")
          .then()
             .statusCode(anyOf(is(400), is(404)))
             .body("success", is(false));
    }

    @Test
    void testGetPlayerByUsernameNotFoundSpanish() {
        given()
          .when().get("/api/leaderboard/non_existent_player_12345")
          .then()
             .statusCode(404)
             .body("success", is(false))
             .body("code", equalTo("PLAYER_NOT_FOUND"))
             .body("message", equalTo("Jugador 'non_existent_player_12345' no encontrado"));
    }

    @Test
    void testGetPlayerByUsernameNotFoundEnglish() {
        given()
          .header("Accept-Language", "en-US,en;q=0.5")
          .when().get("/api/leaderboard/non_existent_player_12345")
          .then()
             .statusCode(404)
             .body("success", is(false))
             .body("code", equalTo("PLAYER_NOT_FOUND"))
             .body("message", equalTo("Player 'non_existent_player_12345' not found"));
    }

    @Test
    void testCreateAndGetPlayer() {
        String username = "test_master_" + System.currentTimeMillis();
        CreatePlayerCommand command = new CreatePlayerCommand(username, 1750, 40, 5);

        given()
          .contentType(ContentType.JSON)
          .body(command)
          .when().post("/api/leaderboard")
          .then()
             .statusCode(201)
             .body("success", is(true))
             .body("data.username", equalTo(username))
             .body("data.elo", equalTo(1750))
             .body("message", equalTo("Jugador creado con éxito"));

        given()
          .when().get("/api/leaderboard/" + username)
          .then()
             .statusCode(200)
             .body("success", is(true))
             .body("data.username", equalTo(username))
             .body("data.wins", equalTo(40))
             .body("message", equalTo("Jugador obtenido con éxito"));
    }

    @Test
    void testCreatePlayerDefaultEloWhenZero() {
        String username = "rookie_" + System.currentTimeMillis();
        String jsonPayload = String.format("{\"username\":\"%s\",\"elo\":0}", username);

        given()
          .contentType(ContentType.JSON)
          .body(jsonPayload)
          .when().post("/api/leaderboard")
          .then()
             .statusCode(201)
             .body("success", is(true))
             .body("data.username", equalTo(username))
             .body("data.elo", equalTo(1200));
    }

    @Test
    void testCreatePlayerNullUsernameSpanish() {
        String jsonPayload = "{\"elo\":1500,\"wins\":5,\"losses\":2}";

        given()
          .contentType(ContentType.JSON)
          .body(jsonPayload)
          .when().post("/api/leaderboard")
          .then()
             .statusCode(400)
             .body("success", is(false))
             .body("code", equalTo("VALIDATION_ERROR"))
             .body("message", equalTo("El nombre de usuario es obligatorio"));
    }

    @Test
    void testCreatePlayerNullUsernameEnglish() {
        String jsonPayload = "{\"elo\":1500,\"wins\":5,\"losses\":2}";

        given()
          .header("Accept-Language", "en")
          .contentType(ContentType.JSON)
          .body(jsonPayload)
          .when().post("/api/leaderboard")
          .then()
             .statusCode(400)
             .body("success", is(false))
             .body("code", equalTo("VALIDATION_ERROR"))
             .body("message", equalTo("Username is required"));
    }

    @Test
    void testCreatePlayerBlankUsername() {
        String jsonPayload = "{\"username\":\"   \",\"elo\":1500}";

        given()
          .contentType(ContentType.JSON)
          .body(jsonPayload)
          .when().post("/api/leaderboard")
          .then()
             .statusCode(400)
             .body("success", is(false))
             .body("code", equalTo("VALIDATION_ERROR"));
    }

    @Test
    void testCreatePlayerEmptyUsername() {
        String jsonPayload = "{\"username\":\"\",\"elo\":1500}";

        given()
          .contentType(ContentType.JSON)
          .body(jsonPayload)
          .when().post("/api/leaderboard")
          .then()
             .statusCode(400)
             .body("success", is(false))
             .body("code", equalTo("VALIDATION_ERROR"));
    }

    @Test
    void testCreatePlayerDuplicateUsernameSpanish() {
        String jsonPayload = "{\"username\":\"amaitasun\",\"elo\":1800}";

        given()
          .contentType(ContentType.JSON)
          .body(jsonPayload)
          .when().post("/api/leaderboard")
          .then()
             .statusCode(409)
             .body("success", is(false))
             .body("code", equalTo("PLAYER_ALREADY_EXISTS"))
             .body("message", equalTo("El jugador 'amaitasun' ya existe"));
    }

    @Test
    void testCreatePlayerDuplicateUsernameEnglish() {
        String jsonPayload = "{\"username\":\"amaitasun\",\"elo\":1800}";

        given()
          .header("Accept-Language", "en")
          .contentType(ContentType.JSON)
          .body(jsonPayload)
          .when().post("/api/leaderboard")
          .then()
             .statusCode(409)
             .body("success", is(false))
             .body("code", equalTo("PLAYER_ALREADY_EXISTS"))
             .body("message", equalTo("Player username 'amaitasun' already exists"));
    }

    @Test
    void testCreatePlayerInvalidJsonPayload() {
        given()
          .contentType(ContentType.JSON)
          .body("{ invalid_json: true ")
          .when().post("/api/leaderboard")
          .then()
             .statusCode(400)
             .body("success", is(false));
    }

    @Test
    void testCreatePlayerNegativeElo() {
        String jsonPayload = "{\"username\":\"negative_elo_user\",\"elo\":-50}";
        given()
          .contentType(ContentType.JSON)
          .body(jsonPayload)
          .when().post("/api/leaderboard")
          .then()
             .statusCode(400)
             .body("success", is(false))
             .body("code", equalTo("VALIDATION_ERROR"))
             .body("message", equalTo("Las victorias y derrotas no pueden ser negativas"));
    }

    @Test
    void testCreatePlayerExcessiveStats() {
        String jsonPayload = "{\"username\":\"stat_cheater\",\"elo\":1500,\"wins\":9999999}";
        given()
          .contentType(ContentType.JSON)
          .body(jsonPayload)
          .when().post("/api/leaderboard")
          .then()
             .statusCode(400)
             .body("success", is(false))
             .body("code", equalTo("VALIDATION_ERROR"))
             .body("message", equalTo("Las victorias o derrotas exceden el máximo permitido (100000)"));
    }
}
