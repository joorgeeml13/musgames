package dev.ordago.auth.domain.exception;

import dev.ordago.shared.domain.exception.DomainException;

public class PasswordResetTokenExpiredException extends DomainException {

    public PasswordResetTokenExpiredException() {
        super("PASSWORD_RESET_TOKEN_EXPIRED", "auth.password_reset_token.expired", 400);
    }
}
