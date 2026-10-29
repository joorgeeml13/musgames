package dev.ordago.auth.domain.exception;

import dev.ordago.shared.domain.exception.DomainException;

public class SessionRevokedException extends DomainException {
    public SessionRevokedException(String messageKey) {
        super("SESSION_REVOKED", messageKey != null ? messageKey : "auth.session_revoked", 401);
    }
}
