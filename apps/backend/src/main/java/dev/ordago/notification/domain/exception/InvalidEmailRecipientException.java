package dev.ordago.notification.domain.exception;

import java.util.List;

public class InvalidEmailRecipientException extends NotificationDomainException {

    public InvalidEmailRecipientException(String details) {
        super(
                "INVALID_EMAIL_RECIPIENT",
                "notification.recipient.invalid",
                400,
                details != null ? List.of(details) : List.of(),
                details != null ? details : ""
        );
    }
}
