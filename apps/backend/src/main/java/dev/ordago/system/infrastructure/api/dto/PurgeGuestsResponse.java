package dev.ordago.system.infrastructure.api.dto;

import java.time.Instant;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Response payload for housekeeping guest accounts purge operations")
public record PurgeGuestsResponse(
        @Schema(description = "Number of guest accounts deleted", example = "15")
        long purgedCount,

        @Schema(description = "Cutoff timestamp older than which guest accounts were deleted", example = "2026-10-25T00:00:00Z")
        Instant cutoff,

        @Schema(description = "Configured retention days", example = "7")
        int retentionDays,

        @Schema(description = "Execution duration in milliseconds", example = "24")
        long executionTimeMs
) {}
