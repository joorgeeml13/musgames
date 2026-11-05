package dev.ordago.auth.infrastructure.api.dto;

import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Request payload for anonymous guest authentication")
public record GuestLoginRequest(
        @Size(max = 128, message = "Device ID cannot exceed 128 characters")
        @Schema(description = "Optional client device identifier", example = "a1b2c3d4-e5f6-7890", maxLength = 128)
        String deviceId,

        @Size(max = 128, message = "Device name cannot exceed 128 characters")
        @Schema(description = "Optional client device description", example = "Chrome on Android", maxLength = 128)
        String deviceName
) {}
