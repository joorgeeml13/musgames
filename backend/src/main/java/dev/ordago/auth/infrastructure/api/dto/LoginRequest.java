package dev.ordago.auth.infrastructure.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "User login request payload")
public record LoginRequest(
        @Schema(description = "User's email address", example = "player@ordago.dev", required = true)
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        String email,

        @Schema(description = "Plain text password", example = "SecureP@ssw0rd!", required = true, maxLength = 128)
        @NotBlank(message = "Password is required")
        @Size(max = 128, message = "Password must not exceed 128 characters")
        String password,

        @Schema(description = "Unique client device identifier", example = "550e8400-e29b-41d4-a716-446655440000")
        String deviceId,

        @Schema(description = "Human-readable device / client name", example = "Chrome on Linux")
        String deviceName
) {}
