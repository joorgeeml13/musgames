package dev.ordago.notification.domain.model;

import dev.ordago.notification.domain.exception.InvalidEmailSubjectException;

public record EmailSubject(String value) {

    public static final int MAX_LENGTH = 200;

    public EmailSubject {
        if (value == null) {
            throw new InvalidEmailSubjectException("Email subject cannot be null");
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new InvalidEmailSubjectException("Email subject cannot be empty");
        }
        if (trimmed.length() > MAX_LENGTH) {
            throw new InvalidEmailSubjectException("Email subject cannot exceed " + MAX_LENGTH + " characters");
        }
        value = trimmed;
    }

    public static EmailSubject of(String value) {
        return new EmailSubject(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
