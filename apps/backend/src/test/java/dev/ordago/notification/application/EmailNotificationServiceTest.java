package dev.ordago.notification.application;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import dev.ordago.notification.domain.exception.InvalidEmailRecipientException;
import dev.ordago.notification.domain.exception.InvalidEmailSubjectException;
import dev.ordago.notification.domain.model.EmailEvent;
import dev.ordago.notification.domain.port.in.SendEmailUseCase.SendEmailCommand;
import dev.ordago.notification.domain.port.out.EmailEventPublisherPort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailNotificationServiceTest {

    private CapturingEmailEventPublisher publisher;
    private EmailNotificationService service;

    @BeforeEach
    void setUp() {
        publisher = new CapturingEmailEventPublisher();
        service = new EmailNotificationService(publisher);
    }

    @Test
    @DisplayName("Constructor should reject null publisher port")
    void constructorShouldRejectNullPublisher() {
        assertThatThrownBy(() -> new EmailNotificationService(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("emailEventPublisherPort must not be null");
    }

    @Test
    @DisplayName("Should publish email event successfully on valid command")
    void shouldPublishEmailEventSuccessfully() {
        // Arrange
        SendEmailCommand command = new SendEmailCommand(
                "Player@Ordago.Dev ",
                "  Bienvenido al juego  ",
                "welcome",
                Map.of("username", "musero_1"),
                "es"
        );

        // Act
        service.sendEmail(command);

        // Assert
        assertThat(publisher.getPublishedEvents()).hasSize(1);
        EmailEvent event = publisher.getPublishedEvents().getFirst();

        assertThat(event.eventId()).isNotNull();
        assertThat(event.eventType()).isEqualTo(EmailEvent.DEFAULT_EVENT_TYPE);
        assertThat(event.occurredOn()).isNotNull();
        assertThat(event.aggregateId()).isEqualTo("player@ordago.dev");
        assertThat(event.recipient()).isEqualTo("player@ordago.dev");
        assertThat(event.subject()).isEqualTo("Bienvenido al juego");
        assertThat(event.template()).isEqualTo("welcome");
        assertThat(event.templateData()).containsEntry("username", "musero_1");
        assertThat(event.language()).isEqualTo("es");
    }

    @Test
    @DisplayName("Should publish welcome email using convenience command factory")
    void shouldPublishWelcomeEmail() {
        // Arrange
        SendEmailCommand command = SendEmailCommand.welcome(
                "welcome@ordago.dev",
                "ChinchonKing",
                "es"
        );

        // Act
        service.sendEmail(command);

        // Assert
        assertThat(publisher.getPublishedEvents()).hasSize(1);
        EmailEvent event = publisher.getPublishedEvents().getFirst();

        assertThat(event.recipient()).isEqualTo("welcome@ordago.dev");
        assertThat(event.subject()).isEqualTo("¡Bienvenido a Órdago!");
        assertThat(event.template()).isEqualTo("welcome");
        assertThat(event.templateData()).containsEntry("username", "ChinchonKing");
        assertThat(event.language()).isEqualTo("es");
    }

    @Test
    @DisplayName("Should publish email verification using convenience command factory")
    void shouldPublishEmailVerification() {
        // Arrange
        SendEmailCommand command = SendEmailCommand.emailVerification(
                "verify@ordago.dev",
                "https://ordago.dev/verify?token=abc-123",
                "en"
        );

        // Act
        service.sendEmail(command);

        // Assert
        assertThat(publisher.getPublishedEvents()).hasSize(1);
        EmailEvent event = publisher.getPublishedEvents().getFirst();

        assertThat(event.recipient()).isEqualTo("verify@ordago.dev");
        assertThat(event.template()).isEqualTo("email-verification");
        assertThat(event.templateData()).containsEntry("verificationUrl", "https://ordago.dev/verify?token=abc-123");
        assertThat(event.language()).isEqualTo("en");
    }

    @Test
    @DisplayName("Should publish password reset using convenience command factory")
    void shouldPublishPasswordReset() {
        // Arrange
        SendEmailCommand command = SendEmailCommand.passwordReset(
                "reset@ordago.dev",
                "https://ordago.dev/reset?token=pwd-456",
                "es"
        );

        // Act
        service.sendEmail(command);

        // Assert
        assertThat(publisher.getPublishedEvents()).hasSize(1);
        EmailEvent event = publisher.getPublishedEvents().getFirst();

        assertThat(event.recipient()).isEqualTo("reset@ordago.dev");
        assertThat(event.template()).isEqualTo("password-reset");
        assertThat(event.templateData()).containsEntry("resetUrl", "https://ordago.dev/reset?token=pwd-456");
        assertThat(event.language()).isEqualTo("es");
    }

    @Test
    @DisplayName("Should publish generic email using convenience command factory")
    void shouldPublishGenericEmail() {
        // Arrange
        SendEmailCommand command = SendEmailCommand.generic(
                "general@ordago.dev",
                "Aviso del Sistema",
                "Mantenimiento programado hoy a las 22:00",
                "es"
        );

        // Act
        service.sendEmail(command);

        // Assert
        assertThat(publisher.getPublishedEvents()).hasSize(1);
        EmailEvent event = publisher.getPublishedEvents().getFirst();

        assertThat(event.recipient()).isEqualTo("general@ordago.dev");
        assertThat(event.subject()).isEqualTo("Aviso del Sistema");
        assertThat(event.template()).isEqualTo("generic");
        assertThat(event.templateData()).containsEntry("content", "Mantenimiento programado hoy a las 22:00");
    }

    @Test
    @DisplayName("Should reject null command")
    void shouldRejectNullCommand() {
        assertThatThrownBy(() -> service.sendEmail(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("command must not be null");

        assertThat(publisher.getPublishedEvents()).isEmpty();
    }

    @Test
    @DisplayName("Should reject command with invalid recipient format")
    void shouldRejectInvalidRecipient() {
        SendEmailCommand command = new SendEmailCommand(
                "invalid-email-address",
                "Asunto válido",
                "generic",
                Map.of(),
                "es"
        );

        assertThatThrownBy(() -> service.sendEmail(command))
                .isInstanceOf(InvalidEmailRecipientException.class);

        assertThat(publisher.getPublishedEvents()).isEmpty();
    }

    @Test
    @DisplayName("Should reject command with blank subject")
    void shouldRejectBlankSubject() {
        SendEmailCommand command = new SendEmailCommand(
                "valid@ordago.dev",
                "   ",
                "generic",
                Map.of(),
                "es"
        );

        assertThatThrownBy(() -> service.sendEmail(command))
                .isInstanceOf(InvalidEmailSubjectException.class);

        assertThat(publisher.getPublishedEvents()).isEmpty();
    }

    @Test
    @DisplayName("Should reject command with subject exceeding 200 characters")
    void shouldRejectSubjectExceedingMaxLength() {
        SendEmailCommand command = new SendEmailCommand(
                "valid@ordago.dev",
                "x".repeat(201),
                "generic",
                Map.of(),
                "es"
        );

        assertThatThrownBy(() -> service.sendEmail(command))
                .isInstanceOf(InvalidEmailSubjectException.class);

        assertThat(publisher.getPublishedEvents()).isEmpty();
    }

    /**
     * In-memory test double for capturing published {@link EmailEvent} instances.
     */
    private static final class CapturingEmailEventPublisher implements EmailEventPublisherPort {

        private final List<EmailEvent> publishedEvents = new ArrayList<>();

        @Override
        public void publish(EmailEvent event) {
            publishedEvents.add(event);
        }

        List<EmailEvent> getPublishedEvents() {
            return Collections.unmodifiableList(publishedEvents);
        }
    }
}
