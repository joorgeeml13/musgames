package dev.ordago.notification.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailTemplateTest {

    @ParameterizedTest
    @CsvSource({
            "WELCOME, welcome",
            "EMAIL_VERIFICATION, email-verification",
            "PASSWORD_RESET, password-reset",
            "GENERIC, generic"
    })
    @DisplayName("Should return expected template name for each enum value")
    void shouldHaveExpectedTemplateName(EmailTemplate template, String expectedName) {
        assertThat(template.templateName()).isEqualTo(expectedName);
    }

    @ParameterizedTest
    @CsvSource({
            "welcome, WELCOME",
            "WELCOME, WELCOME",
            "email-verification, EMAIL_VERIFICATION",
            "EMAIL_VERIFICATION, EMAIL_VERIFICATION",
            "password-reset, PASSWORD_RESET",
            "PASSWORD_RESET, PASSWORD_RESET",
            "generic, GENERIC",
            "GENERIC, GENERIC"
    })
    @DisplayName("Should parse template from string case-insensitively")
    void shouldParseTemplateFromString(String input, EmailTemplate expected) {
        // Act
        EmailTemplate result = EmailTemplate.fromString(input);

        // Assert
        assertThat(result).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"unknown", "invalid_template", "foo-bar"})
    @DisplayName("Should throw IllegalArgumentException when parsing unknown template")
    void shouldThrowOnUnknownTemplate(String unknown) {
        assertThatThrownBy(() -> EmailTemplate.fromString(unknown))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown email template");
    }

    @Test
    @DisplayName("Should throw NullPointerException when template string is null")
    void shouldThrowOnNullTemplate() {
        assertThatThrownBy(() -> EmailTemplate.fromString(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("template must not be null");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @DisplayName("Should throw IllegalArgumentException when template string is blank")
    void shouldThrowOnBlankTemplate(String blank) {
        assertThatThrownBy(() -> EmailTemplate.fromString(blank))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Template cannot be blank");
    }
}
