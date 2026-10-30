package dev.ordago.notification.domain.exception;

import java.util.List;
import dev.ordago.shared.domain.exception.DomainException;

public class NotificationDomainException extends DomainException {

    public NotificationDomainException(String code, String messageKey, int statusCode, List<String> details, Object... args) {
        super(code, messageKey, statusCode, details, args);
    }

    public NotificationDomainException(String code, String messageKey, int statusCode, Object... args) {
        super(code, messageKey, statusCode, args);
    }

    public NotificationDomainException(String code, String messageKey) {
        super(code, messageKey);
    }
}
