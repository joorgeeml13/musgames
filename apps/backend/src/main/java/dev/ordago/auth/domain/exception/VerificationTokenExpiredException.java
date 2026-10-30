package dev.ordago.auth.domain.exception;

import dev.ordago.shared.domain.exception.DomainException;

public class VerificationTokenExpiredException extends DomainException {

    public VerificationTokenExpiredException() {
        super("VERIFICATION_TOKEN_EXPIRED", "auth.verification_token.expired", 400);
    }
}
