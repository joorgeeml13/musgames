package dev.ordago.auth.infrastructure.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Request payload to claim and convert an anonymous guest account using OAuth identity provider")
public record ClaimGuestSocialRequest(
        @NotBlank(message = "Token is required")
        @Schema(description = "OAuth token issued by identity provider (ID token for Google, access token for Discord)", required = true)
        String token,

        @Size(max = 128, message = "Device ID cannot exceed 128 characters")
        @Schema(description = "Optional client device identifier", example = "a1b2c3d4-e5f6-7890", maxLength = 128)
        String deviceId,

        @Size(max = 128, message = "Device name cannot exceed 128 characters")
        @Schema(description = "Optional client device description", example = "Firefox on Linux", maxLength = 128)
        String deviceName
) {}
