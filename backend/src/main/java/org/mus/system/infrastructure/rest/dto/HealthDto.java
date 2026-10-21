package org.mus.system.infrastructure.rest.dto;

public record HealthDto(
        String status,
        String service,
        String timestamp
) {}
