package dev.ordago.auth.infrastructure.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "User registration request payload")
public record RegisterRequest(
        @Schema(description = "User's email address", example = "player@ordago.dev", required = true)
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        String email,

        @Schema(description = "Plain text password (min 8 chars)", example = "SecureP@ssw0rd!", required = true, minLength = 8, maxLength = 128)
        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
        String password,

        @Schema(description = "In-game display name for player tag identity", example = "ManoMaestro", required = true, minLength = 2, maxLength = 24)
        @NotBlank(message = "Game name is required")
        @Size(min = 2, max = 24, message = "Game name must be between 2 and 24 characters")
        String gameName,

        @Schema(description = "Optional tag line (discriminator). If blank, a random 4-char alphanumeric tag is generated", example = "ESP1", maxLength = 6)
        @Size(max = 6, message = "Tag line must not exceed 6 characters")
        String tagLine,

        @Schema(description = "Unique client device identifier", example = "550e8400-e29b-41d4-a716-446655440000")
        String deviceId,

        @Schema(description = "Human-readable device / client name", example = "Chrome on Linux")
        String deviceName
) {}
