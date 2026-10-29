package dev.ordago.auth.domain.exception;

import dev.ordago.shared.domain.exception.DomainException;

public class SessionNotFoundException extends DomainException {
    public SessionNotFoundException() {
        super("SESSION_NOT_FOUND", "auth.session_not_found", 401);
    }
}
