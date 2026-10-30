package dev.ordago.shared.infrastructure.messaging.rabbitmq;

import java.util.Objects;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.jboss.logging.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.ordago.shared.domain.event.DomainEvent;
import dev.ordago.shared.domain.port.out.EventPublisherPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * RabbitMQ adapter implementing {@link EventPublisherPort}.
 * Serializes {@link DomainEvent} to JSON and dispatches to the 'ordago-events-out' channel.
 */
@ApplicationScoped
public class RabbitMqEventPublisherAdapter implements EventPublisherPort {

    private static final Logger LOG = Logger.getLogger(RabbitMqEventPublisherAdapter.class);
    public static final String CHANNEL_NAME = "ordago-events-out";

    private final Emitter<String> emitter;
    private final ObjectMapper objectMapper;

    @Inject
    public RabbitMqEventPublisherAdapter(
            @Channel(CHANNEL_NAME) Emitter<String> emitter,
            ObjectMapper objectMapper
    ) {
        this.emitter = Objects.requireNonNull(emitter, "emitter must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    @Override
    public void publish(DomainEvent event) {
        if (event == null) {
            LOG.warn("Domain event to publish cannot be null; ignoring");
            return;
        }

        try {
            String payload = objectMapper.writeValueAsString(event);
            emitter.send(payload).whenComplete((success, failure) -> {
                if (failure != null) {
                    LOG.errorf(
                            failure,
                            "Failed to deliver domain event to RabbitMQ [eventId=%s, eventType=%s, aggregateId=%s]",
                            event.eventId(),
                            event.eventType(),
                            event.aggregateId()
                    );
                } else {
                    LOG.debugf(
                            "Domain event successfully acknowledged by RabbitMQ [eventId=%s, eventType=%s, aggregateId=%s]",
                            event.eventId(),
                            event.eventType(),
                            event.aggregateId()
                    );
                }
            });
            LOG.debugf(
                    "Emitted domain event to channel '%s' [eventId=%s, eventType=%s, aggregateId=%s]",
                    CHANNEL_NAME,
                    event.eventId(),
                    event.eventType(),
                    event.aggregateId()
            );
        } catch (Exception e) {
            LOG.errorf(
                    e,
                    "Failed to serialize or publish domain event [eventId=%s, eventType=%s, aggregateId=%s]",
                    event.eventId(),
                    event.eventType(),
                    event.aggregateId()
            );
        }
    }
}
