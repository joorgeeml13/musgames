package dev.ordago.notification.domain.port.out;

import dev.ordago.notification.domain.model.EmailEvent;

/**
 * Driven port for publishing email notification domain events.
 */
public interface EmailEventPublisherPort {

    void publish(EmailEvent event);
}
