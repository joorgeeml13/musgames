package dev.ordago.shared.domain.port.out;

import dev.ordago.shared.domain.event.DomainEvent;

/**
 * Driven port for publishing domain events to message brokers or event buses.
 */
public interface EventPublisherPort {

    void publish(DomainEvent event);
}
