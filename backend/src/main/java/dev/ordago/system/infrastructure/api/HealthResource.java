package dev.ordago.system.infrastructure.api;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import dev.ordago.shared.domain.i18n.I18nMessageService;
import dev.ordago.shared.infrastructure.api.response.ApiResponse;
import dev.ordago.system.infrastructure.api.dto.HealthDto;

import java.time.Instant;

@Path("/api/health")
@ApplicationScoped
public class HealthResource {

    @Inject
    I18nMessageService i18nMessageService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkHealth() {
        HealthDto healthDto = new HealthDto("UP", "mus-backend", Instant.now().toString());
        String message = i18nMessageService.getMessage("system.health.ok");
        return Response.ok(ApiResponse.of(healthDto, message)).build();
    }
}
