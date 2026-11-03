package dev.ordago.auth.domain.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import dev.ordago.shared.domain.event.DomainEvent;

public record TokenTheftDetectedEvent(
        UUID eventId,
        String eventType,
        Instant occurredOn,
        String aggregateId,
        UUID accountId,
        UUID sessionId,
        String deviceId,
        String deviceName,
        String clientIp
) implements DomainEvent {

    public static final String DEFAULT_EVENT_TYPE = "auth.security.theft_detected";

    public TokenTheftDetectedEvent {
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
        Objects.requireNonNull(accountId, "accountId must not be null");
        Objects.requireNonNull(sessionId, "sessionId must not be null");
    }

    public static TokenTheftDetectedEvent of(
            UUID accountId,
            UUID sessionId,
            String deviceId,
            String deviceName,
            String clientIp
    ) {
        Objects.requireNonNull(accountId, "accountId must not be null");
        return new TokenTheftDetectedEvent(
                UUID.randomUUID(),
                DEFAULT_EVENT_TYPE,
                Instant.now(),
                accountId.toString(),
                accountId,
                sessionId,
                deviceId,
                deviceName,
                clientIp
        );
    }

    @Override
    public String routingKey() {
        return DEFAULT_EVENT_TYPE;
    }
}
