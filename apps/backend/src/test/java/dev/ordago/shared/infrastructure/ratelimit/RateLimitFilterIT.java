package dev.ordago.shared.infrastructure.ratelimit;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.inject.Inject;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
@TestProfile(RateLimitFilterIT.RateLimitEnabledProfile.class)
@DisplayName("RateLimitFilter Integration Tests")
class RateLimitFilterIT {

    public static class RateLimitEnabledProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "ordago.rate-limit.enabled", "true",
                    "ordago.rate-limit.login.capacity", "5",
                    "ordago.rate-limit.login.duration-minutes", "1",
                    "ordago.rate-limit.register.capacity", "3",
                    "ordago.rate-limit.register.duration-minutes", "1"
            );
        }
    }

    @Inject
    RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        rateLimitService.clearCache();
    }

    @Test
    @DisplayName("Enforces rate limiting on /login, returns 429 when exhausted, and isolates by client IP")
    void enforcesRateLimitOnLoginEndpoint() {
        String testIp = "198.51.100.77";
        String loginPayload = """
                {
                    "email": "ratelimit-target@example.com",
                    "password": "WrongPassword123!",
                    "deviceId": "device-rl-test",
                    "deviceName": "Test Runner"
                }
                """;

        // 1. Consume up to capacity (5 requests) -> all return 401 (normal authentication failure)
        for (int i = 0; i < 5; i++) {
            Response response = given()
                    .header("X-Forwarded-For", testIp)
                    .contentType(ContentType.JSON)
                    .body(loginPayload)
                    .when()
                    .post("/api/v1/auth/login");

            assertThat(response.getStatusCode())
                    .as("Request %d within capacity must not be rate limited (expected 401)", i + 1)
                    .isEqualTo(401);

            assertThat(response.getHeader("X-RateLimit-Limit"))
                    .as("X-RateLimit-Limit header must be 5")
                    .isEqualTo("5");

            assertThat(response.getHeader("X-RateLimit-Remaining"))
                    .as("X-RateLimit-Remaining must decrement")
                    .isEqualTo(String.valueOf(4 - i));
        }

        // 2. Request 6 exceeds capacity -> must return HTTP 429 Too Many Requests
        Response blockedResponse = given()
                .header("X-Forwarded-For", testIp)
                .contentType(ContentType.JSON)
                .body(loginPayload)
                .when()
                .post("/api/v1/auth/login");

        assertThat(blockedResponse.getStatusCode())
                .as("Request exceeding capacity must be blocked with HTTP 429")
                .isEqualTo(429);

        String retryAfterHeader = blockedResponse.getHeader("Retry-After");
        assertThat(retryAfterHeader).isNotNull();
        assertThat(Long.parseLong(retryAfterHeader)).isGreaterThan(0L);

        assertThat(blockedResponse.getHeader("X-RateLimit-Limit")).isEqualTo("5");
        assertThat(blockedResponse.getHeader("X-RateLimit-Remaining")).isEqualTo("0");

        String resetHeader = blockedResponse.getHeader("X-RateLimit-Reset");
        assertThat(resetHeader).isNotNull();
        assertThat(Long.parseLong(resetHeader))
                .isGreaterThanOrEqualTo(System.currentTimeMillis() / 1000L);

        String body = blockedResponse.getBody().asString();
        assertThat(body)
                .as("Response body must contain standard error code TOO_MANY_REQUESTS")
                .contains("TOO_MANY_REQUESTS");
        assertThat(body)
                .as("Response body message must indicate rate limit exceeded")
                .contains("Rate limit exceeded. Try again in");

        // 3. Different client IP -> succeeds without 429
        String differentIp = "198.51.100.88";
        Response diffIpResponse = given()
                .header("X-Forwarded-For", differentIp)
                .contentType(ContentType.JSON)
                .body(loginPayload)
                .when()
                .post("/api/v1/auth/login");

        assertThat(diffIpResponse.getStatusCode())
                .as("Different IP must not be affected by exhausted bucket of previous IP")
                .isEqualTo(401);
        assertThat(diffIpResponse.getHeader("X-RateLimit-Limit")).isEqualTo("5");
        assertThat(diffIpResponse.getHeader("X-RateLimit-Remaining")).isEqualTo("4");
    }

    @Test
    @DisplayName("Rate limits are independent between different tiers (Login vs Register)")
    void rateLimitsAreIndependentAcrossTiers() {
        String testIp = "198.51.100.99";
        String loginPayload = """
                {
                    "email": "tier-target@example.com",
                    "password": "WrongPassword123!",
                    "deviceId": "device-tier-test",
                    "deviceName": "Tier Runner"
                }
                """;

        // Exhaust login tier (5 requests)
        for (int i = 0; i < 5; i++) {
            given()
                    .header("X-Forwarded-For", testIp)
                    .contentType(ContentType.JSON)
                    .body(loginPayload)
                    .when()
                    .post("/api/v1/auth/login")
                    .then()
                    .statusCode(401);
        }

        // 6th login request is rate-limited (429)
        given()
                .header("X-Forwarded-For", testIp)
                .contentType(ContentType.JSON)
                .body(loginPayload)
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(429);

        // Register request from the SAME IP is NOT rate-limited (it has its own bucket)
        String registerPayload = """
                {
                    "email": "fresh-account@example.com",
                    "password": "SecurePassword123!",
                    "gameName": "TierUser",
                    "tagLine": "1234",
                    "deviceId": "device-tier-test",
                    "deviceName": "Tier Runner"
                }
                """;

        Response registerResponse = given()
                .header("X-Forwarded-For", testIp)
                .contentType(ContentType.JSON)
                .body(registerPayload)
                .when()
                .post("/api/v1/auth/register");

        assertThat(registerResponse.getStatusCode())
                .as("Register should not be blocked by exhausted Login tier (expected 201 or 400/409, not 429)")
                .isNotEqualTo(429);
        assertThat(registerResponse.getHeader("X-RateLimit-Limit")).isEqualTo("3");
        assertThat(registerResponse.getHeader("X-RateLimit-Remaining")).isEqualTo("2");
    }
}
