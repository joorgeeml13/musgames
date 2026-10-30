package dev.ordago.auth.domain.exception;

import dev.ordago.shared.domain.exception.ValidationException;

public class InvalidVerificationTokenException extends ValidationException {

    public InvalidVerificationTokenException() {
        super("INVALID_VERIFICATION_TOKEN", "auth.verification_token.invalid");
    }
}
