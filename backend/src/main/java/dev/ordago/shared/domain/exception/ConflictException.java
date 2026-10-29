package dev.ordago.shared.domain.exception;

public class ConflictException extends DomainException {

    public ConflictException(String code, String messageKey, Object... args) {
        super(code, messageKey, 409, args);
    }

    public ConflictException(String messageKey, Object... args) {
        super("CONFLICT", messageKey, 409, args);
    }
}
