package dev.ordago.shared.domain.exception;

import java.util.List;

public class ValidationException extends DomainException {

    public ValidationException(String code, String messageKey, List<String> details, Object... args) {
        super(code, messageKey, 400, details, args);
    }

    public ValidationException(String code, String messageKey, Object... args) {
        super(code, messageKey, 400, args);
    }

    public ValidationException(String messageKey, Object... args) {
        super("VALIDATION_ERROR", messageKey, 400, args);
    }
}
