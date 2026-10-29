package dev.ordago.auth.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import dev.ordago.auth.domain.exception.InvalidEmailException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailTest {

    @Test
    void shouldCreateValidEmailAndNormalizeToLowercaseAndTrimmed() {
        Email email = Email.of("  User.Test+Mus@Example.COM  ");

        assertThat(email.value()).isEqualTo("user.test+mus@example.com");
        assertThat(email.toString()).isEqualTo("user.test+mus@example.com");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "player@ordago.dev",
            "mus_fan123@sub.domain.org",
            "chinchon+test@musgames.es"
    })
    void shouldAcceptValidEmailFormats(String validEmail) {
        Email email = Email.of(validEmail);
        assertThat(email.value()).isEqualTo(validEmail);
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
    void shouldRejectInvalidEmailFormats(String invalidEmail) {
        assertThatThrownBy(() -> Email.of(invalidEmail))
                .isInstanceOf(InvalidEmailException.class);
    }

    @Test
    void shouldRejectNullEmail() {
        assertThatThrownBy(() -> Email.of(null))
                .isInstanceOf(InvalidEmailException.class);
    }

    @Test
    void shouldRejectEmailExceedingMaxLength() {
        String longLocalPart = "a".repeat(250);
        String invalidEmail = longLocalPart + "@domain.com";

        assertThatThrownBy(() -> Email.of(invalidEmail))
                .isInstanceOf(InvalidEmailException.class);
    }

    @Test
    void shouldBeEqualWhenNormalizedValuesMatch() {
        Email email1 = Email.of("PLAYER@Ordago.Dev");
        Email email2 = Email.of("player@ordago.dev");

        assertThat(email1).isEqualTo(email2);
        assertThat(email1.hashCode()).isEqualTo(email2.hashCode());
    }
}
