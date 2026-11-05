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

    public static PasswordHash ofNullable(String value) {
        return (value != null && !value.isBlank()) ? new PasswordHash(value) : null;
    }

    @Override
    public String toString() {
        return "[PROTECTED]";
    }
}
