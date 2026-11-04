package dev.ordago.auth.infrastructure.api;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.http.Cookie;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class UserSessionResourceIT {

    private static final String ACCESS_COOKIE_NAME = "access_token";
    private static final String REFRESH_COOKIE_NAME = "refresh_token";

    private Map<String, Object> buildRegisterPayload(String prefix, String deviceId, String deviceName) {
        String unique = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return Map.of(
                "email", prefix + "_" + unique + "@ordago.dev",
                "password", "SecureSessionPass123!",
                "gameName", "Mus" + unique,
                "tagLine", "SS",
                "deviceId", deviceId,
                "deviceName", deviceName
        );
    }

    private Response register(Map<String, Object> payload) {
        return given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/api/v1/auth/register");
    }

    private Response login(String email, String password, String deviceId, String deviceName) {
        return given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "email", email,
                        "password", password,
                        "deviceId", deviceId,
                        "deviceName", deviceName
                ))
                .when()
                .post("/api/v1/auth/login");
    }

    @Test
    @DisplayName("GET /sessions unauthenticated -> 401 Unauthorized")
    void shouldRejectUnauthenticatedGetSessions() {
        // Act & Assert
        given()
                .when()
                .get("/api/v1/auth/sessions")
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("DELETE /sessions/{id} unauthenticated -> 401 Unauthorized")
    void shouldRejectUnauthenticatedDeleteSessionById() {
        // Act & Assert
        given()
                .when()
                .delete("/api/v1/auth/sessions/" + UUID.randomUUID())
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("DELETE /sessions unauthenticated -> 401 Unauthorized")
    void shouldRejectUnauthenticatedDeleteSessions() {
        // Act & Assert
        given()
                .when()
                .delete("/api/v1/auth/sessions")
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("GET /sessions with 2 active devices -> 200, both listed, exactly 1 marked is_current: true and isCurrent: true")
    void shouldListActiveSessionsAndIdentifyCurrentDevice() {
        Map<String, Object> payload = buildRegisterPayload("multidev", "dev-phone-1", "Pixel 8 Pro");
        Response regResponse = register(payload).then().statusCode(201).extract().response();

        String email = (String) payload.get("email");
        String password = (String) payload.get("password");

        Response loginResponse = login(email, password, "dev-laptop-2", "MacBook Pro")
                .then().statusCode(200).extract().response();

        String laptopAccessToken = loginResponse.path("accessToken");

        Response sessionsResponse = given()
                .header("Authorization", "Bearer " + laptopAccessToken)
                .when()
                .get("/api/v1/auth/sessions")
                .then()
                .statusCode(200)
                .extract().response();

        List<Map<String, Object>> sessions = sessionsResponse.jsonPath().getList("$");
        assertThat(sessions).hasSize(2);

        Map<String, Object> currentSession = sessions.stream()
                .filter(s -> Boolean.TRUE.equals(s.get("is_current")))
                .findFirst()
                .orElse(null);

        assertThat(currentSession).isNotNull();
        assertThat(currentSession.get("deviceId")).isEqualTo("dev-laptop-2");
        assertThat(currentSession.get("deviceName")).isEqualTo("MacBook Pro");
        assertThat(currentSession.get("is_current")).isEqualTo(true);
        assertThat(currentSession.get("isCurrent")).isEqualTo(true);

        Map<String, Object> otherSession = sessions.stream()
                .filter(s -> Boolean.FALSE.equals(s.get("is_current")))
                .findFirst()
                .orElse(null);

        assertThat(otherSession).isNotNull();
        assertThat(otherSession.get("deviceId")).isEqualTo("dev-phone-1");
        assertThat(otherSession.get("deviceName")).isEqualTo("Pixel 8 Pro");
        assertThat(otherSession.get("is_current")).isEqualTo(false);
        assertThat(otherSession.get("isCurrent")).isEqualTo(false);

        long currentCount = sessions.stream()
                .filter(s -> Boolean.TRUE.equals(s.get("is_current")) && Boolean.TRUE.equals(s.get("isCurrent")))
                .count();
        assertThat(currentCount).isEqualTo(1);
    }

    @Test
    @DisplayName("DELETE /sessions/{id} on second device -> 204. Refresh fails with 401. Current session remains valid.")
    void shouldRevokeSpecificDeviceSession() {
        Map<String, Object> payload = buildRegisterPayload("rev_dev", "dev-desktop-1", "Desktop PC");
        Response regResponse = register(payload).then().statusCode(201).extract().response();
        String primaryAccessToken = regResponse.path("accessToken");

        String email = (String) payload.get("email");
        String password = (String) payload.get("password");

        Response loginResponse = login(email, password, "dev-mobile-2", "iPhone 15")
                .then().statusCode(200).extract().response();
        String mobileRefreshToken = loginResponse.path("refreshToken");

        // List sessions using primary desktop token to get mobile sessionId
        Response sessionsResponse = given()
                .header("Authorization", "Bearer " + primaryAccessToken)
                .when()
                .get("/api/v1/auth/sessions")
                .then()
                .statusCode(200)
                .extract().response();

        List<Map<String, Object>> sessions = sessionsResponse.jsonPath().getList("$");
        String mobileSessionId = sessions.stream()
                .filter(s -> "dev-mobile-2".equals(s.get("deviceId")))
                .map(s -> (String) s.get("id"))
                .findFirst()
                .orElseThrow();

        // Primary device revokes the mobile session
        given()
                .header("Authorization", "Bearer " + primaryAccessToken)
                .when()
                .delete("/api/v1/auth/sessions/" + mobileSessionId)
                .then()
                .statusCode(204);

        // Mobile refresh token is now revoked -> returns 401
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("refreshToken", mobileRefreshToken))
                .when()
                .post("/api/v1/auth/refresh")
                .then()
                .statusCode(401);

        // Primary desktop session remains active and valid
        Response updatedSessionsResponse = given()
                .header("Authorization", "Bearer " + primaryAccessToken)
                .when()
                .get("/api/v1/auth/sessions")
                .then()
                .statusCode(200)
                .extract().response();

        List<Map<String, Object>> activeSessions = updatedSessionsResponse.jsonPath().getList("$");
        assertThat(activeSessions).hasSize(1);
        assertThat(activeSessions.getFirst().get("deviceId")).isEqualTo("dev-desktop-1");
        assertThat(activeSessions.getFirst().get("is_current")).isEqualTo(true);
    }

    @Test
    @DisplayName("DELETE /sessions/{id} on another user's session -> 404 Not Found (IDOR defense)")
    void shouldRejectRevocationOfAnotherUsersSessionWith404() {
        Map<String, Object> payload1 = buildRegisterPayload("idor_victim", "dev-v1", "Victim PC");
        Response reg1 = register(payload1).then().statusCode(201).extract().response();
        String victimAccessToken = reg1.path("accessToken");

        Response victimSessions = given()
                .header("Authorization", "Bearer " + victimAccessToken)
                .when()
                .get("/api/v1/auth/sessions")
                .then()
                .statusCode(200)
                .extract().response();

        String victimSessionId = victimSessions.jsonPath().getString("[0].id");

        Map<String, Object> payload2 = buildRegisterPayload("idor_attacker", "dev-a1", "Attacker Phone");
        Response reg2 = register(payload2).then().statusCode(201).extract().response();
        String attackerAccessToken = reg2.path("accessToken");

        // Attacker attempts to delete victim's session -> 404 Not Found
        given()
                .header("Authorization", "Bearer " + attackerAccessToken)
                .when()
                .delete("/api/v1/auth/sessions/" + victimSessionId)
                .then()
                .statusCode(404);

        // Victim's session remains active
        given()
                .header("Authorization", "Bearer " + victimAccessToken)
                .when()
                .get("/api/v1/auth/sessions")
                .then()
                .statusCode(200)
                .body("size()", equalTo(1));
    }

    @Test
    @DisplayName("DELETE /sessions?exclude_current=true -> 204. All other sessions revoked; current stays valid and cookies not cleared.")
    void shouldRevokeOtherSessionsExcludingCurrent() {
        Map<String, Object> payload = buildRegisterPayload("excl_curr", "dev-first", "First Device");
        Response reg = register(payload).then().statusCode(201).extract().response();
        String firstRefreshToken = reg.path("refreshToken");

        String email = (String) payload.get("email");
        String password = (String) payload.get("password");

        Response login2 = login(email, password, "dev-second", "Second Device")
                .then().statusCode(200).extract().response();
        String secondAccessToken = login2.path("accessToken");
        String secondRefreshToken = login2.path("refreshToken");

        Response login3 = login(email, password, "dev-third", "Third Device")
                .then().statusCode(200).extract().response();
        String thirdRefreshToken = login3.path("refreshToken");

        // From second device, revoke all other sessions
        Response deleteResponse = given()
                .header("Authorization", "Bearer " + secondAccessToken)
                .queryParam("exclude_current", true)
                .when()
                .delete("/api/v1/auth/sessions")
                .then()
                .statusCode(204)
                .extract().response();

        // Cookies should NOT be cleared on exclude_current=true
        Cookie accessCookie = deleteResponse.getDetailedCookie(ACCESS_COOKIE_NAME);
        assertThat(accessCookie).isNull();

        // First and third device refresh tokens are revoked -> 401
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("refreshToken", firstRefreshToken))
                .when()
                .post("/api/v1/auth/refresh")
                .then()
                .statusCode(401);

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("refreshToken", thirdRefreshToken))
                .when()
                .post("/api/v1/auth/refresh")
                .then()
                .statusCode(401);

        // Second device remains active
        Response activeSessions = given()
                .header("Authorization", "Bearer " + secondAccessToken)
                .when()
                .get("/api/v1/auth/sessions")
                .then()
                .statusCode(200)
                .extract().response();

        List<Map<String, Object>> list = activeSessions.jsonPath().getList("$");
        assertThat(list).hasSize(1);
        assertThat(list.getFirst().get("deviceId")).isEqualTo("dev-second");
        assertThat(list.getFirst().get("is_current")).isEqualTo(true);

        // Second device can refresh successfully
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("refreshToken", secondRefreshToken))
                .when()
                .post("/api/v1/auth/refresh")
                .then()
                .statusCode(200)
                .body("accessToken", notNullValue());
    }

    @Test
    @DisplayName("DELETE /sessions?exclude_current=false -> 204. All sessions revoked and cookies cleared with Max-Age=0.")
    void shouldRevokeAllSessionsIncludingCurrent() {
        Map<String, Object> payload = buildRegisterPayload("rev_all", "dev-d1", "Device 1");
        Response reg = register(payload).then().statusCode(201).extract().response();
        String d1RefreshToken = reg.path("refreshToken");

        String email = (String) payload.get("email");
        String password = (String) payload.get("password");

        Response login2 = login(email, password, "dev-d2", "Device 2")
                .then().statusCode(200).extract().response();
        String d2AccessToken = login2.path("accessToken");
        String d2RefreshToken = login2.path("refreshToken");

        // Revoke all sessions
        Response deleteResponse = given()
                .header("Authorization", "Bearer " + d2AccessToken)
                .queryParam("exclude_current", false)
                .when()
                .delete("/api/v1/auth/sessions")
                .then()
                .statusCode(204)
                .extract().response();

        // Cookies cleared with Max-Age=0
        Cookie accessCookie = deleteResponse.getDetailedCookie(ACCESS_COOKIE_NAME);
        assertThat(accessCookie).isNotNull();
        assertThat(accessCookie.getMaxAge()).isEqualTo(0);

        Cookie refreshCookie = deleteResponse.getDetailedCookie(REFRESH_COOKIE_NAME);
        assertThat(refreshCookie).isNotNull();
        assertThat(refreshCookie.getMaxAge()).isEqualTo(0);

        // Both device refresh tokens revoked -> 401
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("refreshToken", d1RefreshToken))
                .when()
                .post("/api/v1/auth/refresh")
                .then()
                .statusCode(401);

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("refreshToken", d2RefreshToken))
                .when()
                .post("/api/v1/auth/refresh")
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("DELETE /sessions/{currentId} on current device session -> 204, session revoked and cookies cleared with Max-Age=0.")
    void shouldRevokeCurrentDeviceSessionAndClearCookies() {
        Map<String, Object> payload = buildRegisterPayload("rev_curr_id", "dev-self", "Self Device");
        Response reg = register(payload).then().statusCode(201).extract().response();
        String accessToken = reg.path("accessToken");
        String refreshToken = reg.path("refreshToken");

        Response sessionsResponse = given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get("/api/v1/auth/sessions")
                .then()
                .statusCode(200)
                .extract().response();

        String currentSessionId = sessionsResponse.jsonPath().getString("[0].id");

        // Revoke current session by ID
        Response deleteResponse = given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .delete("/api/v1/auth/sessions/" + currentSessionId)
                .then()
                .statusCode(204)
                .extract().response();

        // Cookies must be cleared
        Cookie accessCookie = deleteResponse.getDetailedCookie(ACCESS_COOKIE_NAME);
        assertThat(accessCookie).isNotNull();
        assertThat(accessCookie.getMaxAge()).isEqualTo(0);

        Cookie refreshCookie = deleteResponse.getDetailedCookie(REFRESH_COOKIE_NAME);
        assertThat(refreshCookie).isNotNull();
        assertThat(refreshCookie.getMaxAge()).isEqualTo(0);

        // Refresh token is revoked -> 401
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("refreshToken", refreshToken))
                .when()
                .post("/api/v1/auth/refresh")
                .then()
                .statusCode(401);
    }
}
