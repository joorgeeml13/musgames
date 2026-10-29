package dev.ordago.system.infrastructure.api.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Health status payload")
public record HealthDto(
        @Schema(description = "Service health status", example = "UP")
        String status,

        @Schema(description = "Application service name", example = "mus-backend")
        String service,

        @Schema(description = "Timestamp of the health check", example = "2026-10-29T15:30:00Z")
        String timestamp
) {}
