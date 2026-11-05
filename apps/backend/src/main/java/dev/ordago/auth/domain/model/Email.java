package dev.ordago.auth.domain.model;

import java.util.Locale;
import java.util.regex.Pattern;
import dev.ordago.auth.domain.exception.InvalidEmailException;

public record Email(String value) {

    private static final int MAX_LENGTH = 255;
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );
    private static final Email EMPTY = new Email(null);

    public Email {
        if (value != null) {
            String trimmed = value.trim();
            if (trimmed.isEmpty()) {
                throw new InvalidEmailException("Email cannot be empty");
            }
            if (trimmed.length() > MAX_LENGTH) {
                throw new InvalidEmailException("Email cannot exceed " + MAX_LENGTH + " characters");
            }
            if (!EMAIL_PATTERN.matcher(trimmed).matches()) {
                throw new InvalidEmailException("Email format is invalid: " + trimmed);
            }
            value = trimmed.toLowerCase(Locale.ROOT);
        }
    }

    public static Email of(String value) {
        if (value == null) {
            throw new InvalidEmailException("Email cannot be null");
        }
        return new Email(value);
    }

    public static Email ofNullable(String value) {
        return (value != null && !value.isBlank()) ? new Email(value) : null;
    }

    public static Email empty() {
        return EMPTY;
    }

    @Override
    public String toString() {
        return value != null ? value : "";
    }
}
