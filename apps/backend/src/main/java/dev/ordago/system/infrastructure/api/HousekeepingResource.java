package dev.ordago.system.infrastructure.api;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Objects;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import dev.ordago.auth.domain.port.in.PurgeExpiredSessionsUseCase;
import dev.ordago.shared.infrastructure.api.response.ApiError;
import dev.ordago.system.infrastructure.api.dto.PurgeSessionsResponse;

@Path("/api/v1/system/housekeeping")
@Tag(name = "System - Housekeeping", description = "System housekeeping operations")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
public class HousekeepingResource {

    public static final String HOUSEKEEPING_HEADER = "X-Housekeeping-Key";

    private final PurgeExpiredSessionsUseCase purgeExpiredSessionsUseCase;
    private final String expectedApiKey;

    @Context
    UriInfo uriInfo;

    public HousekeepingResource(
            PurgeExpiredSessionsUseCase purgeExpiredSessionsUseCase,
            @ConfigProperty(name = "ordago.housekeeping.api-key") String expectedApiKey
    ) {
        this.purgeExpiredSessionsUseCase = Objects.requireNonNull(purgeExpiredSessionsUseCase, "PurgeExpiredSessionsUseCase cannot be null");
        if (expectedApiKey == null || expectedApiKey.trim().length() < 32) {
            throw new IllegalStateException("ordago.housekeeping.api-key must not be blank and must be at least 32 characters long");
        }
        this.expectedApiKey = expectedApiKey;
    }

    @POST
    @Path("/sessions/purge")
    @Operation(
            summary = "Purge expired or revoked user sessions",
            description = "Purges user sessions that have been revoked or expired for longer than the retention window"
    )
    @APIResponses(value = {
            @APIResponse(
                    responseCode = "200",
                    description = "Sessions purged successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PurgeSessionsResponse.class))
            ),
            @APIResponse(
                    responseCode = "400",
                    description = "Invalid retention days parameter",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class))
            ),
            @APIResponse(
                    responseCode = "401",
                    description = "Unauthorized - invalid or missing housekeeping authorization key",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class))
            )
    })
    public Response purgeSessions(
            @HeaderParam(HOUSEKEEPING_HEADER) String apiKeyHeader,
            @QueryParam("retentionDays") @DefaultValue("30") int retentionDays
    ) {
        if (!isAuthorized(apiKeyHeader)) {
            String path = (uriInfo != null) ? uriInfo.getPath() : "/api/v1/system/housekeeping/sessions/purge";
            if (path != null) {
                path = path.replaceAll("[\\r\\n]", "");
                if (!path.startsWith("/")) {
                    path = "/" + path;
                }
            }
            ApiError apiError = new ApiError(
                    401,
                    "Unauthorized",
                    "UNAUTHORIZED",
                    "Invalid or missing housekeeping authorization key",
                    path,
                    List.of()
            );
            return Response.status(Response.Status.UNAUTHORIZED).entity(apiError).build();
        }

        long startTime = System.currentTimeMillis();
        PurgeExpiredSessionsUseCase.PurgeResult result = purgeExpiredSessionsUseCase.purgeRevokedOrExpiredSessions(retentionDays);
        long executionTimeMs = System.currentTimeMillis() - startTime;

        PurgeSessionsResponse responseDto = new PurgeSessionsResponse(
                result.purgedCount(),
                result.cutoff(),
                result.retentionDays(),
                executionTimeMs
        );

        return Response.ok(responseDto).build();
    }

    private boolean isAuthorized(String headerKey) {
        if (headerKey == null || headerKey.isBlank()) {
            return false;
        }
        byte[] expectedBytes = expectedApiKey.getBytes(StandardCharsets.UTF_8);
        byte[] headerBytes = headerKey.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expectedBytes, headerBytes);
    }
}
