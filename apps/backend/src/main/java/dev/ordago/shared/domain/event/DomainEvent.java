package dev.ordago.shared.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Common contract for all domain events across bounded contexts.
 * Domain events represent something significant that occurred in the domain.
 * They are immutable and self-contained.
 */
public interface DomainEvent {

    UUID eventId();

    String eventType();

    Instant occurredOn();

    String aggregateId();
}
