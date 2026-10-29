package dev.ordago.system.infrastructure.api.dto;

public record HealthDto(
        String status,
        String service,
        String timestamp
) {}
