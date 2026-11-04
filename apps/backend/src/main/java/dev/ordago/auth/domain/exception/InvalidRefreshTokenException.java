package dev.ordago.auth.domain.exception;

import dev.ordago.shared.domain.exception.DomainException;

public class InvalidRefreshTokenException extends DomainException {
    public InvalidRefreshTokenException() {
        super("INVALID_REFRESH_TOKEN", "auth.invalid_refresh_token", 401);
    }
}
