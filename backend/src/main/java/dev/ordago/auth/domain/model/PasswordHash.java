package dev.ordago.auth.domain.model;

import dev.ordago.auth.domain.exception.InvalidPasswordHashException;

public record PasswordHash(String value) {

    public PasswordHash {
        if (value == null || value.isBlank()) {
            throw new InvalidPasswordHashException("Password hash cannot be null or blank");
        }
        String trimmed = value.trim();
        if (trimmed.length() > 255) {
            throw new InvalidPasswordHashException("Password hash cannot exceed 255 characters");
        }
        value = trimmed;
    }

    public static PasswordHash of(String value) {
        return new PasswordHash(value);
    }

    @Override
    public String toString() {
        return "[PROTECTED]";
    }
}
