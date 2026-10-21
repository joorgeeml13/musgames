package org.mus.system.infrastructure.rest;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

@QuarkusTest
class HealthResourceTest {

    @Test
    void testHealthEndpointDefaultSpanish() {
        given()
          .when().get("/api/health")
          .then()
             .statusCode(200)
             .body("success", is(true))
             .body("data.status", is("UP"))
             .body("data.service", is("mus-backend"))
             .body("data.timestamp", notNullValue())
             .body("message", equalTo("Servicio en funcionamiento"));
    }

    @Test
    void testHealthEndpointEnglishHeader() {
        given()
          .header("Accept-Language", "en-US,en;q=0.9")
          .when().get("/api/health")
          .then()
             .statusCode(200)
             .body("success", is(true))
             .body("data.status", is("UP"))
             .body("data.service", is("mus-backend"))
             .body("data.timestamp", notNullValue())
             .body("message", equalTo("Service is healthy"));
    }

    @Test
    void testHealthEndpointMethodNotAllowed() {
        given()
          .when().post("/api/health")
          .then()
             .statusCode(405)
             .body("success", is(false))
             .body("status", is(405));
    }
}
