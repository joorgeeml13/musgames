package dev.ordago.auth.infrastructure.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Change password request payload")
public record ChangePasswordRequest(
        @Schema(description = "Current account password", example = "OldP@ssw0rd!", required = true, maxLength = 128)
        @NotBlank(message = "Current password is required")
        @Size(max = 128, message = "Current password must not exceed 128 characters")
        String currentPassword,

        @Schema(description = "New plain text password (8-128 chars)", example = "NewSecureP@ss123", required = true, minLength = 8, maxLength = 128)
        @NotBlank(message = "New password is required")
        @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
        String newPassword
) {}
