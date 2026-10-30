package dev.ordago.shared.infrastructure.messaging.rabbitmq;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.ordago.shared.domain.event.DomainEvent;
import dev.ordago.shared.domain.event.GenericDomainEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RabbitMqEventPublisherAdapterTest {

    private ObjectMapper objectMapper;
    private RecordingEmitter recordingEmitter;
    private RabbitMqEventPublisherAdapter adapter;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        recordingEmitter = new RecordingEmitter();
        adapter = new RabbitMqEventPublisherAdapter(recordingEmitter, objectMapper);
    }

    @Test
    @DisplayName("Should validate constructor dependencies reject nulls")
    void constructorShouldValidateArguments() {
        assertThatThrownBy(() -> new RabbitMqEventPublisherAdapter(null, objectMapper))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("emitter must not be null");

        assertThatThrownBy(() -> new RabbitMqEventPublisherAdapter(recordingEmitter, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("objectMapper must not be null");
    }

    @Test
    @DisplayName("Should serialize DomainEvent to JSON and emit to channel")
    void shouldPublishDomainEventSuccessfully() throws Exception {
        UUID eventId = UUID.randomUUID();
        String eventType = "auth.user.registered";
        Instant now = Instant.parse("2026-10-30T10:15:30.00Z");
        String aggregateId = UUID.randomUUID().toString();

        DomainEvent event = new GenericDomainEvent(eventId, eventType, now, aggregateId);

        adapter.publish(event);

        assertThat(recordingEmitter.getEmittedMessages()).hasSize(1);
        String emittedJson = recordingEmitter.getEmittedMessages().getFirst();

        JsonNode root = objectMapper.readTree(emittedJson);
        assertThat(root.get("eventId").asText()).isEqualTo(eventId.toString());
        assertThat(root.get("eventType").asText()).isEqualTo(eventType);
        assertThat(root.get("aggregateId").asText()).isEqualTo(aggregateId);
        assertThat(root.get("occurredOn")).isNotNull();
    }

    @Test
    @DisplayName("Should serialize custom domain event with extra fields")
    void shouldPublishCustomDomainEventWithExtraPayload() throws Exception {
        UUID eventId = UUID.randomUUID();
        Instant now = Instant.now();
        String aggregateId = "room-mus-42";

        record RoomCreatedDomainEvent(
                UUID eventId,
                String eventType,
                Instant occurredOn,
                String aggregateId,
                String roomName,
                int maxPlayers
        ) implements DomainEvent {}

        RoomCreatedDomainEvent customEvent = new RoomCreatedDomainEvent(
                eventId,
                "room.mus.created",
                now,
                aggregateId,
                "Mesa de los Reyes",
                4
        );

        adapter.publish(customEvent);

        assertThat(recordingEmitter.getEmittedMessages()).hasSize(1);
        JsonNode root = objectMapper.readTree(recordingEmitter.getEmittedMessages().getFirst());
        assertThat(root.get("eventType").asText()).isEqualTo("room.mus.created");
        assertThat(root.get("roomName").asText()).isEqualTo("Mesa de los Reyes");
        assertThat(root.get("maxPlayers").asInt()).isEqualTo(4);
    }

    @Test
    @DisplayName("Should handle null event safely without publishing or throwing")
    void shouldHandleNullEventGracefully() {
        assertThatCode(() -> adapter.publish(null))
                .doesNotThrowAnyException();

        assertThat(recordingEmitter.getEmittedMessages()).isEmpty();
    }

    @Test
    @DisplayName("Should catch synchronous emitter exceptions without crashing caller")
    void shouldCatchSynchronousEmitterException() {
        recordingEmitter.setSynchronousFailure(new IllegalStateException("SmallRye channel is cancelled"));

        GenericDomainEvent event = GenericDomainEvent.create("test.event", "agg-123");

        assertThatCode(() -> adapter.publish(event))
                .doesNotThrowAnyException();

        assertThat(recordingEmitter.getEmittedMessages()).isEmpty();
    }

    @Test
    @DisplayName("Should handle asynchronous emitter completion failures gracefully")
    void shouldHandleAsynchronousEmitterFailure() {
        recordingEmitter.setAsynchronousFailure(new RuntimeException("RabbitMQ connection refused"));

        GenericDomainEvent event = GenericDomainEvent.create("test.event", "agg-123");

        assertThatCode(() -> adapter.publish(event))
                .doesNotThrowAnyException();

        assertThat(recordingEmitter.getEmittedMessages()).hasSize(1);
    }

    @Test
    @DisplayName("Should catch serialization errors without crashing caller")
    void shouldCatchSerializationFailure() {
        DomainEvent unserializableEvent = new DomainEvent() {
            @Override
            public UUID eventId() {
                return UUID.randomUUID();
            }

            @Override
            public String eventType() {
                return "test.unserializable";
            }

            @Override
            public Instant occurredOn() {
                return Instant.now();
            }

            @Override
            public String aggregateId() {
                return "agg-broken";
            }

            // Causes Jackson serialization failure due to recursive reference
            public Object getSelf() {
                return this;
            }
        };

        assertThatCode(() -> adapter.publish(unserializableEvent))
                .doesNotThrowAnyException();

        assertThat(recordingEmitter.getEmittedMessages()).isEmpty();
    }

    /**
     * Test double implementation of {@link Emitter} that records sent strings.
     */
    private static final class RecordingEmitter implements Emitter<String> {

        private final List<String> emittedMessages = new ArrayList<>();
        private RuntimeException synchronousFailure;
        private Throwable asynchronousFailure;

        void setSynchronousFailure(RuntimeException failure) {
            this.synchronousFailure = failure;
        }

        void setAsynchronousFailure(Throwable failure) {
            this.asynchronousFailure = failure;
        }

        List<String> getEmittedMessages() {
            return Collections.unmodifiableList(emittedMessages);
        }

        @Override
        public CompletionStage<Void> send(String msg) {
            if (synchronousFailure != null) {
                throw synchronousFailure;
            }
            emittedMessages.add(msg);
            if (asynchronousFailure != null) {
                return CompletableFuture.failedFuture(asynchronousFailure);
            }
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public <M extends Message<? extends String>> void send(M msg) {
            throw new UnsupportedOperationException("Not implemented for test double");
        }

        @Override
        public void complete() {
            // no-op
        }

        @Override
        public void error(Exception e) {
            // no-op
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean hasRequests() {
            return true;
        }
    }
}
