package dev.ordago.notification.infrastructure.messaging.rabbitmq;

import java.util.Objects;
import org.jboss.logging.Logger;
import dev.ordago.notification.domain.model.EmailEvent;
import dev.ordago.notification.domain.port.out.EmailEventPublisherPort;
import dev.ordago.shared.domain.port.out.EventPublisherPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * RabbitMQ adapter implementing {@link EmailEventPublisherPort}.
 * Delegates serialization and delivery to the shared {@link EventPublisherPort}.
 */
@ApplicationScoped
public class RabbitMqEmailEventPublisherAdapter implements EmailEventPublisherPort {

    private static final Logger LOG = Logger.getLogger(RabbitMqEmailEventPublisherAdapter.class);

    private final EventPublisherPort eventPublisherPort;

    @Inject
    public RabbitMqEmailEventPublisherAdapter(EventPublisherPort eventPublisherPort) {
        this.eventPublisherPort = Objects.requireNonNull(eventPublisherPort, "eventPublisherPort must not be null");
    }

    @Override
    public void publish(EmailEvent event) {
        if (event == null) {
            LOG.warn("Email event to publish cannot be null; ignoring");
            return;
        }

        LOG.debugf(
                "Delegating EmailEvent publication to EventPublisherPort [eventId=%s, template=%s, recipient=%s]",
                event.eventId(),
                event.template(),
                event.recipient()
        );
        eventPublisherPort.publish(event);
    }
}
