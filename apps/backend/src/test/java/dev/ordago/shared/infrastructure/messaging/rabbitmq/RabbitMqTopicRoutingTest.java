package dev.ordago.shared.infrastructure.messaging.rabbitmq;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.ordago.auth.domain.event.TokenTheftDetectedEvent;
import dev.ordago.notification.domain.model.EmailEvent;
import dev.ordago.notification.infrastructure.messaging.rabbitmq.RabbitMqEmailEventPublisherAdapter;
import dev.ordago.shared.domain.event.DomainEvent;
import dev.ordago.shared.domain.event.GenericDomainEvent;
import io.smallrye.reactive.messaging.rabbitmq.OutgoingRabbitMQMetadata;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RabbitMQ Dynamic Topic Routing Keys Test Suite")
class RabbitMqTopicRoutingTest {

    private ObjectMapper objectMapper;
    private CapturingEmitter capturingEmitter;
    private RabbitMqEventPublisherAdapter eventPublisherAdapter;
    private RabbitMqEmailEventPublisherAdapter emailPublisherAdapter;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        capturingEmitter = new CapturingEmitter();
        eventPublisherAdapter = new RabbitMqEventPublisherAdapter(capturingEmitter, objectMapper);
        emailPublisherAdapter = new RabbitMqEmailEventPublisherAdapter(eventPublisherAdapter);
    }

    @Nested
    @DisplayName("1. Email Verification Dynamic Routing")
    class EmailVerificationRoutingTests {

        @Test
        @DisplayName("Should attach 'auth.email.verification' routing key when email-verification event is published directly")
        void shouldAttachAuthEmailVerificationRoutingKeyWhenPublishedDirectly() throws Exception {
            // Arrange
            String recipient = "verify.user@ordago.dev";
            String subject = "Verifica tu cuenta de correo";
            String template = "email-verification";
            Map<String, String> data = Map.of("verificationUrl", "https://ordago.dev/verify?token=abc-123");
            EmailEvent event = EmailEvent.create(recipient, subject, template, data, "es");

            // Act
            eventPublisherAdapter.publish(event);

            // Assert
            assertThat(capturingEmitter.getCapturedMessages()).hasSize(1);
            OutgoingRabbitMQMetadata metadata = capturingEmitter.getLastMetadata();
            assertThat(metadata.getRoutingKey()).isEqualTo("auth.email.verification");
            assertThat(metadata.getTimestamp()).isEqualTo(ZonedDateTime.ofInstant(event.occurredOn(), ZoneOffset.UTC));

            JsonNode payloadJson = objectMapper.readTree(capturingEmitter.getLastPayload());
            assertThat(payloadJson.get("eventId").asText()).isEqualTo(event.eventId().toString());
            assertThat(payloadJson.get("recipient").asText()).isEqualTo(recipient);
            assertThat(payloadJson.get("template").asText()).isEqualTo("email-verification");
            assertThat(payloadJson.get("templateData").get("verificationUrl").asText())
                    .isEqualTo("https://ordago.dev/verify?token=abc-123");
        }

        @Test
        @DisplayName("Should attach 'auth.email.verification' routing key when published via RabbitMqEmailEventPublisherAdapter")
        void shouldAttachAuthEmailVerificationRoutingKeyWhenPublishedViaEmailAdapter() {
            // Arrange
            EmailEvent event = EmailEvent.create(
                    "aggregate-account-1",
                    "player@ordago.dev",
                    "Confirma tu email",
                    "email-verification",
                    Map.of("token", "token-xyz-888"),
                    "es"
            );

            // Act
            emailPublisherAdapter.publish(event);

            // Assert
            assertThat(capturingEmitter.getCapturedMessages()).hasSize(1);
            OutgoingRabbitMQMetadata metadata = capturingEmitter.getLastMetadata();
            assertThat(metadata.getRoutingKey()).isEqualTo("auth.email.verification");
        }
    }

    @Nested
    @DisplayName("2. Password Reset Dynamic Routing")
    class PasswordResetRoutingTests {

        @Test
        @DisplayName("Should attach 'auth.email.password_reset' routing key when password-reset event is published directly")
        void shouldAttachAuthEmailPasswordResetRoutingKeyWhenPublishedDirectly() throws Exception {
            // Arrange
            String recipient = "reset.user@ordago.dev";
            String subject = "Recuperar contraseña de Órdago";
            String template = "password-reset";
            Map<String, String> data = Map.of("resetUrl", "https://ordago.dev/reset-password?token=secret-token-99");
            EmailEvent event = EmailEvent.create(recipient, subject, template, data, "es");

            // Act
            eventPublisherAdapter.publish(event);

            // Assert
            assertThat(capturingEmitter.getCapturedMessages()).hasSize(1);
            OutgoingRabbitMQMetadata metadata = capturingEmitter.getLastMetadata();
            assertThat(metadata.getRoutingKey()).isEqualTo("auth.email.password_reset");
            assertThat(metadata.getTimestamp()).isEqualTo(ZonedDateTime.ofInstant(event.occurredOn(), ZoneOffset.UTC));

            JsonNode payloadJson = objectMapper.readTree(capturingEmitter.getLastPayload());
            assertThat(payloadJson.get("eventId").asText()).isEqualTo(event.eventId().toString());
            assertThat(payloadJson.get("recipient").asText()).isEqualTo(recipient);
            assertThat(payloadJson.get("template").asText()).isEqualTo("password-reset");
            assertThat(payloadJson.get("templateData").get("resetUrl").asText())
                    .isEqualTo("https://ordago.dev/reset-password?token=secret-token-99");
        }

        @Test
        @DisplayName("Should attach 'auth.email.password_reset' routing key when published via RabbitMqEmailEventPublisherAdapter")
        void shouldAttachAuthEmailPasswordResetRoutingKeyWhenPublishedViaEmailAdapter() {
            // Arrange
            EmailEvent event = EmailEvent.create(
                    "aggregate-account-2",
                    "player2@ordago.dev",
                    "Restablecimiento de contraseña",
                    "password-reset",
                    Map.of("token", "rst-555"),
                    "en"
            );

            // Act
            emailPublisherAdapter.publish(event);

            // Assert
            assertThat(capturingEmitter.getCapturedMessages()).hasSize(1);
            OutgoingRabbitMQMetadata metadata = capturingEmitter.getLastMetadata();
            assertThat(metadata.getRoutingKey()).isEqualTo("auth.email.password_reset");
        }
    }

    @Nested
    @DisplayName("3. Welcome Email Dynamic Routing")
    class WelcomeEmailRoutingTests {

        @Test
        @DisplayName("Should attach 'auth.email.welcome' routing key when welcome event is published directly")
        void shouldAttachAuthEmailWelcomeRoutingKeyWhenPublishedDirectly() throws Exception {
            // Arrange
            String recipient = "welcome.user@ordago.dev";
            String subject = "¡Bienvenido a Órdago!";
            String template = "welcome";
            Map<String, String> data = Map.of("gameName", "chiquito_mus", "tagLine", "MAD");
            EmailEvent event = EmailEvent.create(recipient, subject, template, data, "es");

            // Act
            eventPublisherAdapter.publish(event);

            // Assert
            assertThat(capturingEmitter.getCapturedMessages()).hasSize(1);
            OutgoingRabbitMQMetadata metadata = capturingEmitter.getLastMetadata();
            assertThat(metadata.getRoutingKey()).isEqualTo("auth.email.welcome");
            assertThat(metadata.getTimestamp()).isEqualTo(ZonedDateTime.ofInstant(event.occurredOn(), ZoneOffset.UTC));

            JsonNode payloadJson = objectMapper.readTree(capturingEmitter.getLastPayload());
            assertThat(payloadJson.get("eventId").asText()).isEqualTo(event.eventId().toString());
            assertThat(payloadJson.get("recipient").asText()).isEqualTo(recipient);
            assertThat(payloadJson.get("template").asText()).isEqualTo("welcome");
            assertThat(payloadJson.get("templateData").get("gameName").asText()).isEqualTo("chiquito_mus");
        }

        @Test
        @DisplayName("Should attach 'auth.email.welcome' routing key when published via RabbitMqEmailEventPublisherAdapter")
        void shouldAttachAuthEmailWelcomeRoutingKeyWhenPublishedViaEmailAdapter() {
            // Arrange
            EmailEvent event = EmailEvent.create(
                    "aggregate-account-3",
                    "player3@ordago.dev",
                    "Welcome to Órdago Mus",
                    "welcome",
                    Map.of("username", "mus_player_3"),
                    "en"
            );

            // Act
            emailPublisherAdapter.publish(event);

            // Assert
            assertThat(capturingEmitter.getCapturedMessages()).hasSize(1);
            OutgoingRabbitMQMetadata metadata = capturingEmitter.getLastMetadata();
            assertThat(metadata.getRoutingKey()).isEqualTo("auth.email.welcome");
        }
    }

    @Nested
    @DisplayName("4. Token Theft Detection Dynamic Routing")
    class TokenTheftDetectionRoutingTests {

        @Test
        @DisplayName("Should attach 'auth.security.theft_detected' routing key when token theft event is published")
        void shouldAttachAuthSecurityTheftDetectedRoutingKey() throws Exception {
            // Arrange
            UUID accountId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            String deviceId = "device-pixel-8";
            String deviceName = "Pixel 8 Pro";
            String clientIp = "198.51.100.42";

            TokenTheftDetectedEvent event = TokenTheftDetectedEvent.of(
                    accountId,
                    sessionId,
                    deviceId,
                    deviceName,
                    clientIp
            );

            // Act
            eventPublisherAdapter.publish(event);

            // Assert
            assertThat(capturingEmitter.getCapturedMessages()).hasSize(1);
            OutgoingRabbitMQMetadata metadata = capturingEmitter.getLastMetadata();
            assertThat(metadata.getRoutingKey()).isEqualTo("auth.security.theft_detected");
            assertThat(metadata.getTimestamp()).isEqualTo(ZonedDateTime.ofInstant(event.occurredOn(), ZoneOffset.UTC));

            JsonNode payloadJson = objectMapper.readTree(capturingEmitter.getLastPayload());
            assertThat(payloadJson.get("eventId").asText()).isEqualTo(event.eventId().toString());
            assertThat(payloadJson.get("eventType").asText()).isEqualTo(TokenTheftDetectedEvent.DEFAULT_EVENT_TYPE);
            assertThat(payloadJson.get("accountId").asText()).isEqualTo(accountId.toString());
            assertThat(payloadJson.get("sessionId").asText()).isEqualTo(sessionId.toString());
            assertThat(payloadJson.get("deviceId").asText()).isEqualTo(deviceId);
            assertThat(payloadJson.get("deviceName").asText()).isEqualTo(deviceName);
            assertThat(payloadJson.get("clientIp").asText()).isEqualTo(clientIp);
        }

        @Test
        @DisplayName("Should correctly route token theft event when created via explicit constructor")
        void shouldRouteExplicitConstructorTokenTheftEvent() {
            // Arrange
            UUID eventId = UUID.randomUUID();
            Instant occurredOn = Instant.parse("2026-11-03T11:45:00.00Z");
            UUID accountId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            TokenTheftDetectedEvent event = new TokenTheftDetectedEvent(
                    eventId,
                    "auth.security.theft_detected",
                    occurredOn,
                    accountId.toString(),
                    accountId,
                    sessionId,
                    "desktop-firefox",
                    "Firefox on Linux",
                    "10.0.0.1"
            );

            // Act
            eventPublisherAdapter.publish(event);

            // Assert
            OutgoingRabbitMQMetadata metadata = capturingEmitter.getLastMetadata();
            assertThat(metadata.getRoutingKey()).isEqualTo("auth.security.theft_detected");
            assertThat(metadata.getTimestamp()).isEqualTo(ZonedDateTime.ofInstant(occurredOn, ZoneOffset.UTC));
        }
    }

    @Nested
    @DisplayName("5. Fallback Behavior: Generic Domain Events")
    class FallbackRoutingTests {

        @Test
        @DisplayName("Should use eventType as routing key when routingKey is not overridden in GenericDomainEvent")
        void shouldFallbackToEventTypeForGenericDomainEvent() throws Exception {
            // Arrange
            UUID eventId = UUID.randomUUID();
            String eventType = "room.table.created";
            Instant occurredOn = Instant.parse("2026-11-03T10:00:00.00Z");
            String aggregateId = "table-123";
            GenericDomainEvent event = new GenericDomainEvent(eventId, eventType, occurredOn, aggregateId);

            // Act
            eventPublisherAdapter.publish(event);

            // Assert
            assertThat(capturingEmitter.getCapturedMessages()).hasSize(1);
            OutgoingRabbitMQMetadata metadata = capturingEmitter.getLastMetadata();
            assertThat(metadata.getRoutingKey()).isEqualTo("room.table.created");
            assertThat(metadata.getTimestamp()).isEqualTo(ZonedDateTime.ofInstant(occurredOn, ZoneOffset.UTC));

            JsonNode payloadJson = objectMapper.readTree(capturingEmitter.getLastPayload());
            assertThat(payloadJson.get("eventId").asText()).isEqualTo(eventId.toString());
            assertThat(payloadJson.get("eventType").asText()).isEqualTo(eventType);
            assertThat(payloadJson.get("aggregateId").asText()).isEqualTo(aggregateId);
        }

        @Test
        @DisplayName("Should use eventType as routing key when routingKey is not overridden in custom DomainEvent implementation")
        void shouldFallbackToEventTypeForCustomDomainEventWithoutRoutingKeyOverride() {
            // Arrange
            record MusHandFinishedEvent(
                    UUID eventId,
                    String eventType,
                    Instant occurredOn,
                    String aggregateId,
                    int teamScore,
                    String winningLance
            ) implements DomainEvent {}

            MusHandFinishedEvent event = new MusHandFinishedEvent(
                    UUID.randomUUID(),
                    "game.mus.hand_finished",
                    Instant.now(),
                    "game-room-42",
                    40,
                    "ORDAGO"
            );

            // Act
            eventPublisherAdapter.publish(event);

            // Assert
            assertThat(capturingEmitter.getCapturedMessages()).hasSize(1);
            OutgoingRabbitMQMetadata metadata = capturingEmitter.getLastMetadata();
            assertThat(metadata.getRoutingKey()).isEqualTo("game.mus.hand_finished");
        }

        @ParameterizedTest(name = "eventType=''{0}'' should fallback as dynamic routing key")
        @ValueSource(strings = {
                "user.account.created",
                "user.account.locked",
                "game.room.player_joined",
                "game.room.player_left",
                "match.rating.elo_updated"
        })
        @DisplayName("Should use eventType as fallback routing key across diverse domain event types")
        void shouldFallbackToEventTypeAcrossDiverseEventTypes(String eventType) {
            // Arrange
            GenericDomainEvent event = GenericDomainEvent.create(eventType, UUID.randomUUID().toString());

            // Act
            eventPublisherAdapter.publish(event);

            // Assert
            OutgoingRabbitMQMetadata metadata = capturingEmitter.getLastMetadata();
            assertThat(metadata.getRoutingKey()).isEqualTo(eventType);
        }

        @ParameterizedTest(name = "template=''{0}'' should route to ''{1}''")
        @CsvSource({
                "account-locked, notification.email.account_locked",
                "device-security-alert, notification.email.device_security_alert",
                "newsletter, notification.email.newsletter",
                "generic, notification.email.generic"
        })
        @DisplayName("Should route other notification email templates using normalized template fallback")
        void shouldRouteOtherNotificationEmailTemplatesWithNormalizedRoutingKey(String template, String expectedRoutingKey) {
            // Arrange
            EmailEvent event = EmailEvent.create(
                    "user@ordago.dev",
                    "Subject",
                    template,
                    Map.of("key", "val"),
                    "es"
            );

            // Act
            eventPublisherAdapter.publish(event);

            // Assert
            OutgoingRabbitMQMetadata metadata = capturingEmitter.getLastMetadata();
            assertThat(metadata.getRoutingKey()).isEqualTo(expectedRoutingKey);
        }
    }

    @Nested
    @DisplayName("6. Boundary, Sequence and Metadata Invariants")
    class SequenceAndBoundaryTests {

        @Test
        @DisplayName("Should publish multiple events in sequence without cross-event state contamination")
        void shouldPublishMultipleEventsSequentiallyWithoutStateContamination() {
            // Arrange
            EmailEvent verificationEvent = EmailEvent.create(
                    "u1@ordago.dev", "Verify", "email-verification", Map.of(), "es"
            );
            TokenTheftDetectedEvent theftEvent = TokenTheftDetectedEvent.of(
                    UUID.randomUUID(), UUID.randomUUID(), "dev-1", "Phone", "127.0.0.1"
            );
            EmailEvent resetEvent = EmailEvent.create(
                    "u2@ordago.dev", "Reset", "password-reset", Map.of(), "es"
            );
            GenericDomainEvent genericEvent = GenericDomainEvent.create(
                    "room.closed", "room-1"
            );
            EmailEvent welcomeEvent = EmailEvent.create(
                    "u3@ordago.dev", "Welcome", "welcome", Map.of(), "es"
            );

            // Act
            eventPublisherAdapter.publish(verificationEvent);
            eventPublisherAdapter.publish(theftEvent);
            eventPublisherAdapter.publish(resetEvent);
            eventPublisherAdapter.publish(genericEvent);
            eventPublisherAdapter.publish(welcomeEvent);

            // Assert
            List<Message<? extends String>> messages = capturingEmitter.getCapturedMessages();
            assertThat(messages).hasSize(5);

            assertThat(extractRoutingKey(messages.get(0))).isEqualTo("auth.email.verification");
            assertThat(extractRoutingKey(messages.get(1))).isEqualTo("auth.security.theft_detected");
            assertThat(extractRoutingKey(messages.get(2))).isEqualTo("auth.email.password_reset");
            assertThat(extractRoutingKey(messages.get(3))).isEqualTo("room.closed");
            assertThat(extractRoutingKey(messages.get(4))).isEqualTo("auth.email.welcome");
        }

        private String extractRoutingKey(Message<? extends String> message) {
            return message.getMetadata(OutgoingRabbitMQMetadata.class)
                    .orElseThrow(() -> new AssertionError("Missing OutgoingRabbitMQMetadata"))
                    .getRoutingKey();
        }
    }

    /**
     * Test double for {@link Emitter} capturing raw reactive messaging {@link Message} instances.
     */
    private static final class CapturingEmitter implements Emitter<String> {

        private final List<Message<? extends String>> capturedMessages = new ArrayList<>();

        @Override
        public CompletionStage<Void> send(String msg) {
            throw new UnsupportedOperationException("RabbitMqEventPublisherAdapter must send Message<?> with metadata");
        }

        @Override
        public <M extends Message<? extends String>> void send(M msg) {
            capturedMessages.add(msg);
            msg.ack();
        }

        List<Message<? extends String>> getCapturedMessages() {
            return Collections.unmodifiableList(capturedMessages);
        }

        Message<? extends String> getLastMessage() {
            if (capturedMessages.isEmpty()) {
                throw new IllegalStateException("No messages captured");
            }
            return capturedMessages.getLast();
        }

        OutgoingRabbitMQMetadata getLastMetadata() {
            return getLastMessage()
                    .getMetadata(OutgoingRabbitMQMetadata.class)
                    .orElseThrow(() -> new AssertionError("Message did not contain OutgoingRabbitMQMetadata"));
        }

        String getLastPayload() {
            return getLastMessage().getPayload();
        }

        @Override
        public void complete() {}

        @Override
        public void error(Exception e) {}

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
