package dev.ordago.shared.infrastructure.messaging.rabbitmq;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Metadata;
import org.jboss.logging.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.ordago.shared.domain.event.DomainEvent;
import dev.ordago.shared.domain.port.out.EventPublisherPort;
import io.smallrye.reactive.messaging.rabbitmq.OutgoingRabbitMQMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * RabbitMQ adapter implementing {@link EventPublisherPort}.
 * Serializes {@link DomainEvent} to JSON and dispatches to the 'ordago-events-out' channel
 * with dynamic topic exchange routing keys and message metadata.
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
            String rawRoutingKey = event.routingKey();
            if (rawRoutingKey == null || rawRoutingKey.isBlank()) {
                rawRoutingKey = (event.eventType() != null && !event.eventType().isBlank())
                        ? event.eventType()
                        : "ordago.event";
            }
            rawRoutingKey = rawRoutingKey.trim().replaceAll("[^a-zA-Z0-9_.-]", "_");
            if (rawRoutingKey.length() > 255) {
                rawRoutingKey = rawRoutingKey.substring(0, 255);
            }
            String routingKey = rawRoutingKey;

            Instant occurredOn = event.occurredOn() != null ? event.occurredOn() : Instant.now();
            OutgoingRabbitMQMetadata rabbitMetadata = new OutgoingRabbitMQMetadata.Builder()
                    .withRoutingKey(routingKey)
                    .withTimestamp(ZonedDateTime.ofInstant(occurredOn, ZoneOffset.UTC))
                    .build();

            Message<String> message = Message.of(payload, Metadata.of(rabbitMetadata))
                    .withAck(() -> {
                        LOG.debugf("Domain event acknowledged by RabbitMQ [eventId=%s, routingKey=%s]", event.eventId(), routingKey);
                        return CompletableFuture.completedFuture(null);
                    })
                    .withNack(throwable -> {
                        LOG.errorf(throwable, "Failed to deliver domain event to RabbitMQ [eventId=%s, routingKey=%s]", event.eventId(), routingKey);
                        return CompletableFuture.completedFuture(null);
                    });

            emitter.send(message);
            LOG.debugf(
                    "Emitted domain event to channel '%s' [eventId=%s, routingKey=%s, aggregateId=%s]",
                    CHANNEL_NAME,
                    event.eventId(),
                    routingKey,
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
