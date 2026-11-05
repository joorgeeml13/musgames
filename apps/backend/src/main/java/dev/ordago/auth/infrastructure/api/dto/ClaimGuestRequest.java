package dev.ordago.auth.infrastructure.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Request payload to claim and convert an anonymous guest account into a regular registered account")
public record ClaimGuestRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Email format is invalid")
        @Schema(description = "Account email address", example = "player@ordago.dev", required = true)
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
        @Schema(description = "Plain text password", example = "SecurePassword123!", required = true, minLength = 8, maxLength = 128)
        String password,

        @Size(max = 128, message = "Device ID cannot exceed 128 characters")
        @Schema(description = "Optional client device identifier", example = "a1b2c3d4-e5f6-7890", maxLength = 128)
        String deviceId,

        @Size(max = 128, message = "Device name cannot exceed 128 characters")
        @Schema(description = "Optional client device description", example = "Firefox on Linux", maxLength = 128)
        String deviceName
) {}
