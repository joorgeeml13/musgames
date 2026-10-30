package dev.ordago.notification.domain.model;

import java.util.Locale;
import java.util.regex.Pattern;
import dev.ordago.notification.domain.exception.InvalidEmailRecipientException;

public record EmailRecipient(String value) {

    public static final int MAX_LENGTH = 255;
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );

    public EmailRecipient {
        if (value == null) {
            throw new InvalidEmailRecipientException("Recipient email cannot be null");
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new InvalidEmailRecipientException("Recipient email cannot be empty");
        }
        if (trimmed.length() > MAX_LENGTH) {
            throw new InvalidEmailRecipientException("Recipient email cannot exceed " + MAX_LENGTH + " characters");
        }
        if (!EMAIL_PATTERN.matcher(trimmed).matches()) {
            throw new InvalidEmailRecipientException("Recipient email format is invalid: " + trimmed);
        }
        value = trimmed.toLowerCase(Locale.ROOT);
    }

    public static EmailRecipient of(String value) {
        return new EmailRecipient(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
