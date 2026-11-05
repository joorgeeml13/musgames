package dev.ordago.auth.domain.exception;

import dev.ordago.shared.domain.exception.DomainException;

public class InvalidOAuthTokenException extends DomainException {

    public InvalidOAuthTokenException() {
        super("INVALID_OAUTH_TOKEN", "auth.oauth.invalid_token", 401);
    }

    public InvalidOAuthTokenException(String message) {
        super("INVALID_OAUTH_TOKEN", message, 401);
    }
}
