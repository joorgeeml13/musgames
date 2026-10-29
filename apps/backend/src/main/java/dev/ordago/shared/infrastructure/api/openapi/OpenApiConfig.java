package dev.ordago.shared.infrastructure.api.openapi;

import jakarta.ws.rs.core.Application;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@OpenAPIDefinition(
        info = @Info(
                title = "Órdago Mus API",
                version = "1.0.0-SNAPSHOT",
                description = "REST and WebSocket API for Órdago online Mus game platform"
        ),
        tags = {
                @Tag(name = "Authentication", description = "Operations for registration, login, token refresh and session lifecycle"),
                @Tag(name = "User Profiles", description = "Operations for user profiles and Riot ID public identities"),
                @Tag(name = "System", description = "System health check endpoints")
        }
)
@SecurityScheme(
        securitySchemeName = "jwtAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Enter JWT Bearer token"
)
public class OpenApiConfig extends Application {
}
