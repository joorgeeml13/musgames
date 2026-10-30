package dev.ordago.notification.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import dev.ordago.notification.domain.exception.InvalidEmailRecipientException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailRecipientTest {

    @Test
    @DisplayName("Should create valid EmailRecipient and normalize to lowercase and trimmed")
    void shouldCreateValidEmailRecipientAndNormalize() {
        // Arrange & Act
        EmailRecipient recipient = EmailRecipient.of("  User.Test+Mus@Example.COM  ");

        // Assert
        assertThat(recipient.value()).isEqualTo("user.test+mus@example.com");
        assertThat(recipient.toString()).isEqualTo("user.test+mus@example.com");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "player@ordago.dev",
            "mus_fan123@sub.domain.org",
            "chinchon+test@musgames.es",
            "admin@test.io"
    })
    @DisplayName("Should accept valid email formats")
    void shouldAcceptValidEmailFormats(String validEmail) {
        // Arrange & Act
        EmailRecipient recipient = EmailRecipient.of(validEmail);

        // Assert
        assertThat(recipient.value()).isEqualTo(validEmail);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "   ",
            "not-an-email",
            "missing-domain@",
            "@missing-user.com",
            "user@domain",
            "user@domain..com",
            "user with spaces@domain.com"
    })
    @DisplayName("Should reject invalid email formats")
    void shouldRejectInvalidEmailFormats(String invalidEmail) {
        // Act & Assert
        assertThatThrownBy(() -> EmailRecipient.of(invalidEmail))
                .isInstanceOf(InvalidEmailRecipientException.class)
                .hasMessage("notification.recipient.invalid");
    }

    @Test
    @DisplayName("Should reject null email recipient")
    void shouldRejectNullEmail() {
        // Act & Assert
        assertThatThrownBy(() -> EmailRecipient.of(null))
                .isInstanceOf(InvalidEmailRecipientException.class)
                .hasMessage("notification.recipient.invalid");
    }

    @Test
    @DisplayName("Should reject email exceeding max length of 255 chars")
    void shouldRejectEmailExceedingMaxLength() {
        // Arrange
        String longLocalPart = "a".repeat(250);
        String invalidEmail = longLocalPart + "@domain.com";

        // Act & Assert
        assertThatThrownBy(() -> EmailRecipient.of(invalidEmail))
                .isInstanceOf(InvalidEmailRecipientException.class)
                .hasMessage("notification.recipient.invalid");
    }

    @Test
    @DisplayName("Should be equal when normalized values match")
    void shouldBeEqualWhenNormalizedValuesMatch() {
        // Arrange
        EmailRecipient recipient1 = EmailRecipient.of("NOTIF@Ordago.Dev");
        EmailRecipient recipient2 = EmailRecipient.of("notif@ordago.dev");

        // Assert
        assertThat(recipient1).isEqualTo(recipient2);
        assertThat(recipient1.hashCode()).isEqualTo(recipient2.hashCode());
    }
}
