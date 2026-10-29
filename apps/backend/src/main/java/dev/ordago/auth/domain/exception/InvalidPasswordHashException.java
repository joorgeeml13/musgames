package dev.ordago.auth.domain.exception;

import dev.ordago.shared.domain.exception.ValidationException;

public class InvalidPasswordHashException extends ValidationException {
    public InvalidPasswordHashException(String details) {
        super("INVALID_PASSWORD_HASH", "auth.password_hash.invalid", details);
    }
}
