package dev.ordago.notification.domain.model;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import dev.ordago.shared.domain.event.DomainEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailEventTest {

    @Test
    @DisplayName("Should create EmailEvent via static factory with recipient as aggregateId")
    void shouldCreateEmailEventWithRecipientAsAggregateId() {
        // Arrange
        String recipient = "player@ordago.dev";
        String subject = "Bienvenido a Órdago";
        String template = "welcome";
        Map<String, String> data = Map.of("username", "mus_player");
        String language = "es";

        // Act
        EmailEvent event = EmailEvent.create(recipient, subject, template, data, language);

        // Assert
        assertThat(event).isInstanceOf(DomainEvent.class);
        assertThat(event.eventId()).isNotNull();
        assertThat(event.eventType()).isEqualTo(EmailEvent.DEFAULT_EVENT_TYPE);
        assertThat(event.occurredOn()).isNotNull();
        assertThat(event.aggregateId()).isEqualTo(recipient);
        assertThat(event.recipient()).isEqualTo(recipient);
        assertThat(event.subject()).isEqualTo(subject);
        assertThat(event.template()).isEqualTo(template);
        assertThat(event.templateData()).containsEntry("username", "mus_player");
        assertThat(event.language()).isEqualTo("es");
    }

    @Test
    @DisplayName("Should create EmailEvent via static factory with explicit aggregateId")
    void shouldCreateEmailEventWithExplicitAggregateId() {
        // Arrange
        String aggregateId = "acc-12345";
        String recipient = "player@ordago.dev";
        String subject = "Recuperar contraseña";
        String template = "password-reset";
        Map<String, String> data = Map.of("token", "xyz-789");
        String language = "EN";

        // Act
        EmailEvent event = EmailEvent.create(aggregateId, recipient, subject, template, data, language);

        // Assert
        assertThat(event.aggregateId()).isEqualTo("acc-12345");
        assertThat(event.recipient()).isEqualTo(recipient);
        assertThat(event.language()).isEqualTo("en");
    }

    @Test
    @DisplayName("Should default language to 'es' when language is null or blank")
    void shouldDefaultLanguageWhenNullOrBlank() {
        // Act
        EmailEvent event1 = EmailEvent.create("user@domain.com", "Asunto", "generic", Map.of(), null);
        EmailEvent event2 = EmailEvent.create("user@domain.com", "Asunto", "generic", Map.of(), "   ");

        // Assert
        assertThat(event1.language()).isEqualTo(EmailEvent.DEFAULT_LANGUAGE);
        assertThat(event2.language()).isEqualTo(EmailEvent.DEFAULT_LANGUAGE);
    }

    @Test
    @DisplayName("Should make templateData defensively unmodifiable")
    void shouldEnsureTemplateDataImmutability() {
        // Arrange
        Map<String, String> mutableMap = new HashMap<>();
        mutableMap.put("key1", "val1");

        // Act
        EmailEvent event = EmailEvent.create("user@domain.com", "Subject", "generic", mutableMap, "es");
        mutableMap.put("key2", "val2");

        // Assert: event templateData is unchanged by modifying source map
        assertThat(event.templateData()).hasSize(1);
        assertThat(event.templateData()).doesNotContainKey("key2");

        // Assert: attempting to mutate event's templateData throws UnsupportedOperationException
        assertThatThrownBy(() -> event.templateData().put("key3", "val3"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Should handle null templateData gracefully by creating an empty map")
    void shouldHandleNullTemplateDataGracefully() {
        // Act
        EmailEvent event = EmailEvent.create("user@domain.com", "Subject", "generic", null, "es");

        // Assert
        assertThat(event.templateData()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Should reject null eventId")
    void shouldRejectNullEventId() {
        assertThatThrownBy(() -> new EmailEvent(
                null,
                EmailEvent.DEFAULT_EVENT_TYPE,
                Instant.now(),
                "agg-1",
                "u@d.com",
                "sub",
                "welcome",
                Map.of(),
                "es"
        )).isInstanceOf(NullPointerException.class).hasMessageContaining("eventId must not be null");
    }

    @Test
    @DisplayName("Should reject null or blank eventType")
    void shouldRejectNullOrBlankEventType() {
        assertThatThrownBy(() -> new EmailEvent(
                UUID.randomUUID(),
                null,
                Instant.now(),
                "agg-1",
                "u@d.com",
                "sub",
                "welcome",
                Map.of(),
                "es"
        )).isInstanceOf(NullPointerException.class).hasMessageContaining("eventType must not be null");

        assertThatThrownBy(() -> new EmailEvent(
                UUID.randomUUID(),
                "  ",
                Instant.now(),
                "agg-1",
                "u@d.com",
                "sub",
                "welcome",
                Map.of(),
                "es"
        )).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("eventType must not be blank");
    }

    @Test
    @DisplayName("Should reject null occurredOn")
    void shouldRejectNullOccurredOn() {
        assertThatThrownBy(() -> new EmailEvent(
                UUID.randomUUID(),
                EmailEvent.DEFAULT_EVENT_TYPE,
                null,
                "agg-1",
                "u@d.com",
                "sub",
                "welcome",
                Map.of(),
                "es"
        )).isInstanceOf(NullPointerException.class).hasMessageContaining("occurredOn must not be null");
    }

    @Test
    @DisplayName("Should reject null or blank aggregateId")
    void shouldRejectNullOrBlankAggregateId() {
        assertThatThrownBy(() -> new EmailEvent(
                UUID.randomUUID(),
                EmailEvent.DEFAULT_EVENT_TYPE,
                Instant.now(),
                null,
                "u@d.com",
                "sub",
                "welcome",
                Map.of(),
                "es"
        )).isInstanceOf(NullPointerException.class).hasMessageContaining("aggregateId must not be null");

        assertThatThrownBy(() -> new EmailEvent(
                UUID.randomUUID(),
                EmailEvent.DEFAULT_EVENT_TYPE,
                Instant.now(),
                "  ",
                "u@d.com",
                "sub",
                "welcome",
                Map.of(),
                "es"
        )).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("aggregateId must not be blank");
    }

    @Test
    @DisplayName("Should reject null or blank recipient")
    void shouldRejectNullOrBlankRecipient() {
        assertThatThrownBy(() -> new EmailEvent(
                UUID.randomUUID(),
                EmailEvent.DEFAULT_EVENT_TYPE,
                Instant.now(),
                "agg-1",
                null,
                "sub",
                "welcome",
                Map.of(),
                "es"
        )).isInstanceOf(NullPointerException.class).hasMessageContaining("recipient must not be null");

        assertThatThrownBy(() -> new EmailEvent(
                UUID.randomUUID(),
                EmailEvent.DEFAULT_EVENT_TYPE,
                Instant.now(),
                "agg-1",
                "  ",
                "sub",
                "welcome",
                Map.of(),
                "es"
        )).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("recipient must not be blank");
    }

    @Test
    @DisplayName("Should reject null or blank subject")
    void shouldRejectNullOrBlankSubject() {
        assertThatThrownBy(() -> new EmailEvent(
                UUID.randomUUID(),
                EmailEvent.DEFAULT_EVENT_TYPE,
                Instant.now(),
                "agg-1",
                "u@d.com",
                null,
                "welcome",
                Map.of(),
                "es"
        )).isInstanceOf(NullPointerException.class).hasMessageContaining("subject must not be null");

        assertThatThrownBy(() -> new EmailEvent(
                UUID.randomUUID(),
                EmailEvent.DEFAULT_EVENT_TYPE,
                Instant.now(),
                "agg-1",
                "u@d.com",
                "  ",
                "welcome",
                Map.of(),
                "es"
        )).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("subject must not be blank");
    }

    @Test
    @DisplayName("Should reject null or blank template")
    void shouldRejectNullOrBlankTemplate() {
        assertThatThrownBy(() -> new EmailEvent(
                UUID.randomUUID(),
                EmailEvent.DEFAULT_EVENT_TYPE,
                Instant.now(),
                "agg-1",
                "u@d.com",
                "sub",
                null,
                Map.of(),
                "es"
        )).isInstanceOf(NullPointerException.class).hasMessageContaining("template must not be null");

        assertThatThrownBy(() -> new EmailEvent(
                UUID.randomUUID(),
                EmailEvent.DEFAULT_EVENT_TYPE,
                Instant.now(),
                "agg-1",
                "u@d.com",
                "sub",
                "  ",
                Map.of(),
                "es"
        )).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("template must not be blank");
    }
}
