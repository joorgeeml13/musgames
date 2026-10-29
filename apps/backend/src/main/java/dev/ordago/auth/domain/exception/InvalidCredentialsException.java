package dev.ordago.auth.domain.exception;

import dev.ordago.shared.domain.exception.DomainException;

public class InvalidCredentialsException extends DomainException {
    public InvalidCredentialsException() {
        super("INVALID_CREDENTIALS", "auth.invalid_credentials", 401);
    }
}
