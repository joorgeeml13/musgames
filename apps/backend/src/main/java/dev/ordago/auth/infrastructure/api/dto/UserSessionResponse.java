package dev.ordago.auth.infrastructure.api.dto;

import java.time.Instant;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.ordago.auth.domain.model.UserSession;

@Schema(description = "Information about an active user device session")
public record UserSessionResponse(
        @Schema(description = "Session identifier", example = "d9f8e7c6-b5a4-3210-fedc-ba9876543210")
        UUID id,

        @Schema(description = "Client device identifier", example = "dev-pixel-8-pro")
        String deviceId,

        @Schema(description = "Human-readable device name or browser description", example = "Chrome on Windows")
        String deviceName,

        @Schema(description = "Timestamp when session was created", example = "2026-11-04T12:00:00Z")
        Instant createdAt,

        @Schema(description = "Timestamp when session was last refreshed/active", example = "2026-11-04T14:30:00Z")
        Instant lastActiveAt,

        @JsonProperty("is_current")
        @Schema(description = "Indicates whether this session belongs to the current request's access token", example = "true")
        boolean isCurrent
) {
    @JsonProperty("isCurrent")
    public boolean getIsCurrent() {
        return isCurrent;
    }

    public static UserSessionResponse of(UserSession session, UUID currentSessionId) {
        boolean isCurrent = currentSessionId != null && session.getId().equals(currentSessionId);
        return new UserSessionResponse(
                session.getId(),
                session.getDeviceId(),
                session.getDeviceName(),
                session.getCreatedAt(),
                session.getLastRefreshedAt(),
                isCurrent
        );
    }
}
