package dev.ordago.auth.domain.model;

import org.junit.jupiter.api.Test;
import dev.ordago.auth.domain.exception.InvalidPasswordHashException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordHashTest {

    @Test
    void shouldCreateValidPasswordHashAndTrim() {
        String hash = "  $argon2id$v=19$m=65536,t=3,p=1$c2FsdDEyMzQ1Njc4$aGFzaDEyMzQ1Njc4  ";
        PasswordHash passwordHash = PasswordHash.of(hash);

        assertThat(passwordHash.value()).isEqualTo(hash.trim());
        assertThat(passwordHash.toString()).isEqualTo("[PROTECTED]");
    }

    @Test
    void shouldRejectNullOrBlankPasswordHash() {
        assertThatThrownBy(() -> PasswordHash.of(null))
                .isInstanceOf(InvalidPasswordHashException.class);

        assertThatThrownBy(() -> PasswordHash.of("   "))
                .isInstanceOf(InvalidPasswordHashException.class);
    }

    @Test
    void shouldRejectPasswordHashExceedingMaxLength() {
        String tooLong = "x".repeat(256);
        assertThatThrownBy(() -> PasswordHash.of(tooLong))
                .isInstanceOf(InvalidPasswordHashException.class);
    }

    @Test
    void shouldBeEqualWhenValuesMatch() {
        PasswordHash hash1 = PasswordHash.of("$argon2id$test");
        PasswordHash hash2 = PasswordHash.of("$argon2id$test");

        assertThat(hash1).isEqualTo(hash2);
        assertThat(hash1.hashCode()).isEqualTo(hash2.hashCode());
    }
}
