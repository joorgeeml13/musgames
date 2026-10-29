package dev.ordago.system.infrastructure.api;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

@QuarkusTest
class MetricsResourceTest {

    @Test
    @DisplayName("Prometheus metrics endpoint /q/metrics should return HTTP 200 and prometheus formatted text")
    void testPrometheusMetricsEndpoint() {
        given()
                .when().get("/q/metrics")
                .then()
                .statusCode(200)
                .body(containsString("# HELP"));
    }
}
