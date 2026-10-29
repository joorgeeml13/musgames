package dev.ordago.auth.infrastructure.api.dto;

import jakarta.validation.constraints.NotBlank;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Token refresh request payload")
public record RefreshTokenRequest(
        @Schema(description = "Opaque refresh token string", example = "d9f8e7c6-b5a4-3210-fedc-ba9876543210", required = true)
        @NotBlank(message = "Refresh token is required")
        String refreshToken
) {}
