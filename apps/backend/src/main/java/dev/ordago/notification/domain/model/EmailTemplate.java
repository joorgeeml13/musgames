package dev.ordago.notification.domain.model;

import java.util.Locale;
import java.util.Objects;

public enum EmailTemplate {
    WELCOME("welcome"),
    EMAIL_VERIFICATION("email-verification"),
    PASSWORD_RESET("password-reset"),
    GENERIC("generic");

    private final String templateName;

    EmailTemplate(String templateName) {
        this.templateName = templateName;
    }

    public String templateName() {
        return templateName;
    }

    public static EmailTemplate fromString(String template) {
        Objects.requireNonNull(template, "template must not be null");
        String trimmed = template.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Template cannot be blank");
        }
        String normalized = trimmed.toUpperCase(Locale.ROOT).replace('-', '_');
        for (EmailTemplate value : values()) {
            if (value.name().equals(normalized) || value.templateName.equalsIgnoreCase(trimmed)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown email template: " + template);
    }
}
