package dev.ordago.system.infrastructure.api;

import java.time.Instant;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import dev.ordago.shared.domain.i18n.I18nMessageService;
import dev.ordago.shared.infrastructure.api.response.ApiResponse;
import dev.ordago.system.infrastructure.api.dto.HealthDto;

@Path("/api/health")
@Tag(name = "System", description = "System health check endpoints")
@ApplicationScoped
public class HealthResource {

    @Inject
    I18nMessageService i18nMessageService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "System health check", description = "Returns service availability and status")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Service is operational",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiResponse.class)))
    })
    public Response checkHealth() {
        HealthDto healthDto = new HealthDto("UP", "mus-backend", Instant.now().toString());
        String message = i18nMessageService.getMessage("system.health.ok");
        return Response.ok(ApiResponse.of(healthDto, message)).build();
    }
}
