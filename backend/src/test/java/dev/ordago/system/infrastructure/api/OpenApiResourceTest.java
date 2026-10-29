package dev.ordago.system.infrastructure.api;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;

@QuarkusTest
class OpenApiResourceTest {

    @Test
    @DisplayName("OpenAPI schema endpoint should return valid schema definition and security schemes")
    void testOpenApiEndpoint() {
        given()
                .header("Accept", "application/json")
                .when().get("/q/openapi")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("info.title", equalTo("Órdago Mus API"))
                .body("info.version", equalTo("1.0.0-SNAPSHOT"))
                .body("components.securitySchemes.jwtAuth.type", equalTo("http"))
                .body("components.securitySchemes.jwtAuth.scheme", equalTo("bearer"))
                .body("components.securitySchemes.jwtAuth.bearerFormat", equalTo("JWT"))
                .body("tags.name", org.hamcrest.Matchers.hasItems("Authentication", "User Profiles", "System"))
                .body(containsString("/api/v1/auth/register"))
                .body(containsString("/api/v1/auth/login"))
                .body(containsString("/api/v1/auth/refresh"))
                .body(containsString("/api/v1/auth/logout"))
                .body(containsString("/api/v1/auth/me"))
                .body(containsString("/api/v1/profiles/me"))
                .body(containsString("/api/v1/profiles/{gameName}/{tagLine}"))
                .body(containsString("/api/health"));
    }

    @Test
    @DisplayName("Swagger UI endpoint should be accessible and serve HTML")
    void testSwaggerUiEndpoint() {
        given()
                .when().get("/q/swagger-ui")
                .then()
                .statusCode(200)
                .contentType(containsString("text/html"));
    }
}
