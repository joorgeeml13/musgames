package dev.ordago.auth.infrastructure.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Resend verification email request payload")
public record ResendVerificationRequest(
        @Schema(description = "Account email address to resend verification to", example = "player@ordago.dev", required = true)
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        String email
) {}
