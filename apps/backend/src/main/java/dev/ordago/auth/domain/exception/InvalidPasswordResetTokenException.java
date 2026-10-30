package dev.ordago.auth.domain.exception;

import dev.ordago.shared.domain.exception.ValidationException;

public class InvalidPasswordResetTokenException extends ValidationException {

    public InvalidPasswordResetTokenException() {
        super("INVALID_PASSWORD_RESET_TOKEN", "auth.password_reset_token.invalid");
    }
}
