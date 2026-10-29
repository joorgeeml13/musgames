package dev.ordago.auth.domain.exception;

import dev.ordago.shared.domain.exception.ValidationException;

public class InvalidEmailException extends ValidationException {
    public InvalidEmailException(String details) {
        super("INVALID_EMAIL", "auth.email.invalid", details);
    }
}
