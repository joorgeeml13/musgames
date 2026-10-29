package dev.ordago.auth.domain.exception;

import dev.ordago.shared.domain.exception.DomainException;

public class SessionExpiredException extends DomainException {
    public SessionExpiredException() {
        super("SESSION_EXPIRED", "auth.session_expired", 401);
    }
}
