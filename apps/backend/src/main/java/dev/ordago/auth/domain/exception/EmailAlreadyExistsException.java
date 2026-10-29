package dev.ordago.auth.domain.exception;

import dev.ordago.shared.domain.exception.DomainException;

public class EmailAlreadyExistsException extends DomainException {
    public EmailAlreadyExistsException(String email) {
        super("EMAIL_ALREADY_EXISTS", "auth.email.already_exists", 409, email);
    }
}
