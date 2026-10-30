package dev.ordago.shared.domain.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Standard immutable record implementation of {@link DomainEvent}.
 */
public record GenericDomainEvent(
        UUID eventId,
        String eventType,
        Instant occurredOn,
        String aggregateId
) implements DomainEvent {

    public GenericDomainEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(eventType, "eventType must not be null");
        Objects.requireNonNull(occurredOn, "occurredOn must not be null");
        Objects.requireNonNull(aggregateId, "aggregateId must not be null");
    }

    public static GenericDomainEvent create(String eventType, String aggregateId) {
        return new GenericDomainEvent(UUID.randomUUID(), eventType, Instant.now(), aggregateId);
    }
}
