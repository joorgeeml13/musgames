package dev.ordago.auth.infrastructure.api.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Logout request payload")
public record LogoutRequest(
        @Schema(description = "Optional refresh token to invalidate specifically", example = "d9f8e7c6-b5a4-3210-fedc-ba9876543210")
        String refreshToken
) {}
