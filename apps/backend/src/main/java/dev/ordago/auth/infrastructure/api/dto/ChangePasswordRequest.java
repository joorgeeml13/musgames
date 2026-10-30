package dev.ordago.auth.infrastructure.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Change password request payload")
public record ChangePasswordRequest(
        @Schema(description = "Current account password", example = "OldP@ssw0rd!", required = true)
        @NotBlank(message = "Current password is required")
        String currentPassword,

        @Schema(description = "New plain text password (8-72 chars)", example = "NewSecureP@ss123", required = true, minLength = 8, maxLength = 72)
        @NotBlank(message = "New password is required")
        @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
        String newPassword
) {}
