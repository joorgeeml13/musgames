package dev.ordago.notification.domain.model;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import dev.ordago.shared.domain.event.DomainEvent;

public record EmailEvent(
        UUID eventId,
        String eventType,
        Instant occurredOn,
        String aggregateId,
        String recipient,
        String subject,
        String template,
        Map<String, String> templateData,
        String language
) implements DomainEvent {

    public static final String DEFAULT_EVENT_TYPE = "notification.email.requested";
    public static final String DEFAULT_LANGUAGE = "es";

    public EmailEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(eventType, "eventType must not be null");
        if (eventType.isBlank()) {
            throw new IllegalArgumentException("eventType must not be blank");
        }
        Objects.requireNonNull(occurredOn, "occurredOn must not be null");
        Objects.requireNonNull(aggregateId, "aggregateId must not be null");
        if (aggregateId.isBlank()) {
            throw new IllegalArgumentException("aggregateId must not be blank");
        }
        Objects.requireNonNull(recipient, "recipient must not be null");
        if (recipient.isBlank()) {
            throw new IllegalArgumentException("recipient must not be blank");
        }
        Objects.requireNonNull(subject, "subject must not be null");
        if (subject.isBlank()) {
            throw new IllegalArgumentException("subject must not be blank");
        }
        Objects.requireNonNull(template, "template must not be null");
        if (template.isBlank()) {
            throw new IllegalArgumentException("template must not be blank");
        }
        templateData = templateData != null ? Map.copyOf(templateData) : Map.of();
        language = (language != null && !language.isBlank()) ? language.trim().toLowerCase(Locale.ROOT) : DEFAULT_LANGUAGE;
    }

    public static EmailEvent create(
            String aggregateId,
            String recipient,
            String subject,
            String template,
            Map<String, String> templateData,
            String language
    ) {
        return new EmailEvent(
                UUID.randomUUID(),
                DEFAULT_EVENT_TYPE,
                Instant.now(),
                aggregateId,
                recipient,
                subject,
                template,
                templateData,
                language
        );
    }

    public static EmailEvent create(
            String recipient,
            String subject,
            String template,
            Map<String, String> templateData,
            String language
    ) {
        return create(recipient, recipient, subject, template, templateData, language);
    }
}
