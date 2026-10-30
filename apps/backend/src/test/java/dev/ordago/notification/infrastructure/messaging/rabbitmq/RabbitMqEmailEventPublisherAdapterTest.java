package dev.ordago.notification.infrastructure.messaging.rabbitmq;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import dev.ordago.notification.domain.model.EmailEvent;
import dev.ordago.shared.domain.event.DomainEvent;
import dev.ordago.shared.domain.port.out.EventPublisherPort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RabbitMqEmailEventPublisherAdapterTest {

    private RecordingSharedEventPublisher sharedPublisher;
    private RabbitMqEmailEventPublisherAdapter adapter;

    @BeforeEach
    void setUp() {
        sharedPublisher = new RecordingSharedEventPublisher();
        adapter = new RabbitMqEmailEventPublisherAdapter(sharedPublisher);
    }

    @Test
    @DisplayName("Constructor should reject null EventPublisherPort")
    void constructorShouldRejectNullEventPublisherPort() {
        assertThatThrownBy(() -> new RabbitMqEmailEventPublisherAdapter(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("eventPublisherPort must not be null");
    }

    @Test
    @DisplayName("Should delegate EmailEvent publishing to EventPublisherPort")
    void shouldDelegateEmailEventPublishing() {
        // Arrange
        EmailEvent event = EmailEvent.create(
                "player@ordago.dev",
                "Bienvenido",
                "welcome",
                Map.of("username", "musero_test"),
                "es"
        );

        // Act
        adapter.publish(event);

        // Assert
        assertThat(sharedPublisher.getPublishedEvents()).hasSize(1);
        DomainEvent published = sharedPublisher.getPublishedEvents().getFirst();
        assertThat(published).isSameAs(event);
        assertThat(published.eventType()).isEqualTo(EmailEvent.DEFAULT_EVENT_TYPE);
        assertThat(published.aggregateId()).isEqualTo("player@ordago.dev");
    }

    @Test
    @DisplayName("Should handle null event safely without publishing or throwing")
    void shouldHandleNullEventSafely() {
        // Act & Assert
        assertThatCode(() -> adapter.publish(null))
                .doesNotThrowAnyException();

        assertThat(sharedPublisher.getPublishedEvents()).isEmpty();
    }

    /**
     * Test double for {@link EventPublisherPort}.
     */
    private static final class RecordingSharedEventPublisher implements EventPublisherPort {

        private final List<DomainEvent> publishedEvents = new ArrayList<>();

        @Override
        public void publish(DomainEvent event) {
            publishedEvents.add(event);
        }

        List<DomainEvent> getPublishedEvents() {
            return Collections.unmodifiableList(publishedEvents);
        }
    }
}
