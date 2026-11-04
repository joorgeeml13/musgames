package dev.ordago.system.infrastructure.api.dto;

import java.time.Instant;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Response payload for housekeeping session purge operations")
public record PurgeSessionsResponse(
        @Schema(description = "Number of sessions deleted", example = "42")
        int purgedCount,

        @Schema(description = "Cutoff timestamp older than which sessions were deleted", example = "2026-10-01T00:00:00Z")
        Instant cutoff,

        @Schema(description = "Configured retention days", example = "30")
        int retentionDays,

        @Schema(description = "Execution duration in milliseconds", example = "15")
        long executionTimeMs
) {}
