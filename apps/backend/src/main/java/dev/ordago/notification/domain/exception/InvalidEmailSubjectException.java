package dev.ordago.notification.domain.exception;

import java.util.List;

public class InvalidEmailSubjectException extends NotificationDomainException {

    public InvalidEmailSubjectException(String details) {
        super(
                "INVALID_EMAIL_SUBJECT",
                "notification.subject.invalid",
                400,
                details != null ? List.of(details) : List.of(),
                details != null ? details : ""
        );
    }
}
