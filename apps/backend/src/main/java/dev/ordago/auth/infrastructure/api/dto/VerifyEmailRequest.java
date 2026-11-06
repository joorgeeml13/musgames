package dev.ordago.auth.infrastructure.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Email verification request payload")
public record VerifyEmailRequest(
        @Schema(description = "Verification token sent to user's email", required = true)
        @NotBlank(message = "Token is required")
        @Size(max = 255, message = "Token cannot exceed 255 characters")
        String token
) {}
