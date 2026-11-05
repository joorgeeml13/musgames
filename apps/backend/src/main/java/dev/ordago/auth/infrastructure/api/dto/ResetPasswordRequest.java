package dev.ordago.auth.infrastructure.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Reset password request payload")
public record ResetPasswordRequest(
        @Schema(description = "Password reset token sent via email", required = true)
        @NotBlank(message = "Token is required")
        String token,

        @Schema(description = "New plain text password (8-128 chars)", example = "NewSecureP@ss123", required = true, minLength = 8, maxLength = 128)
        @NotBlank(message = "New password is required")
        @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
        String newPassword
) {}
