package dev.ordago.auth.infrastructure.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
        String password,

        @NotBlank(message = "Game name is required")
        @Size(min = 2, max = 24, message = "Game name must be between 2 and 24 characters")
        String gameName,

        @Size(max = 6, message = "Tag line must not exceed 6 characters")
        String tagLine,

        String deviceId,
        String deviceName
) {}
