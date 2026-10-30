package dev.ordago.notification.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import dev.ordago.notification.domain.exception.InvalidEmailSubjectException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailSubjectTest {

    @Test
    @DisplayName("Should create valid EmailSubject and trim leading/trailing whitespace")
    void shouldCreateValidEmailSubjectAndTrim() {
        // Arrange & Act
        EmailSubject subject = EmailSubject.of("  Bienvenido al juego de Mus  ");

        // Assert
        assertThat(subject.value()).isEqualTo("Bienvenido al juego de Mus");
        assertThat(subject.toString()).isEqualTo("Bienvenido al juego de Mus");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Activación de cuenta",
            "Código de recuperación: 123456",
            "Aviso importante del sistema"
    })
    @DisplayName("Should accept valid email subjects")
    void shouldAcceptValidEmailSubjects(String validSubject) {
        // Arrange & Act
        EmailSubject subject = EmailSubject.of(validSubject);

        // Assert
        assertThat(subject.value()).isEqualTo(validSubject);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "   ",
            "\t\n"
    })
    @DisplayName("Should reject blank email subjects")
    void shouldRejectBlankEmailSubjects(String blankSubject) {
        // Act & Assert
        assertThatThrownBy(() -> EmailSubject.of(blankSubject))
                .isInstanceOf(InvalidEmailSubjectException.class)
                .hasMessage("notification.subject.invalid");
    }

    @Test
    @DisplayName("Should reject null email subject")
    void shouldRejectNullEmailSubject() {
        // Act & Assert
        assertThatThrownBy(() -> EmailSubject.of(null))
                .isInstanceOf(InvalidEmailSubjectException.class)
                .hasMessage("notification.subject.invalid");
    }

    @Test
    @DisplayName("Should reject subject exceeding max length of 200 chars")
    void shouldRejectSubjectExceedingMaxLength() {
        // Arrange
        String longSubject = "a".repeat(201);

        // Act & Assert
        assertThatThrownBy(() -> EmailSubject.of(longSubject))
                .isInstanceOf(InvalidEmailSubjectException.class)
                .hasMessage("notification.subject.invalid");
    }

    @Test
    @DisplayName("Should allow subject with exactly 200 chars")
    void shouldAllowSubjectWithExactlyMaxLength() {
        // Arrange
        String maxSubject = "a".repeat(200);

        // Act
        EmailSubject subject = EmailSubject.of(maxSubject);

        // Assert
        assertThat(subject.value()).hasSize(200);
    }

    @Test
    @DisplayName("Should be equal when values match")
    void shouldBeEqualWhenValuesMatch() {
        // Arrange
        EmailSubject subject1 = EmailSubject.of("Notificación Órdago");
        EmailSubject subject2 = EmailSubject.of("Notificación Órdago");

        // Assert
        assertThat(subject1).isEqualTo(subject2);
        assertThat(subject1.hashCode()).isEqualTo(subject2.hashCode());
    }
}
