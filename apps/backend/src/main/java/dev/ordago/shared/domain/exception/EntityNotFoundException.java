package dev.ordago.shared.domain.exception;

public class EntityNotFoundException extends DomainException {

    public EntityNotFoundException(String code, String messageKey, Object... args) {
        super(code, messageKey, 404, args);
    }

    public EntityNotFoundException(String messageKey, Object... args) {
        super("ENTITY_NOT_FOUND", messageKey, 404, args);
    }
}
