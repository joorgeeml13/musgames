package dev.ordago.auth.infrastructure.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Forgot password request payload")
public record ForgotPasswordRequest(
        @Schema(description = "Account email address to request password reset for", example = "player@ordago.dev", required = true)
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        String email
) {}
