package dev.ordago.auth.infrastructure.api.dto;

public record LogoutRequest(
        String refreshToken
) {}
