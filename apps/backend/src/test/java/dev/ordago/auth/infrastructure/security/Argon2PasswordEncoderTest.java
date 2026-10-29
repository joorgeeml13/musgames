package dev.ordago.auth.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class Argon2PasswordEncoderTest {

    private Argon2PasswordEncoder encoder;

    @BeforeEach
    void setUp() {
        // Use faster parameters for testing: 1024 KB memory, 1 iteration
        encoder = new Argon2PasswordEncoder(1024, 1, 1, 16, 32);
    }

    @Test
    void shouldEncodeAndMatchPassword() {
        String rawPassword = "CorrectHorseBatteryStaple1!";
        String hash = encoder.encode(rawPassword);

        assertThat(hash).isNotNull().startsWith("$argon2id$v=19$");
        assertThat(encoder.matches(rawPassword, hash)).isTrue();
        assertThat(encoder.matches("WrongPassword", hash)).isFalse();
    }

    @Test
    void shouldGenerateUniqueSaltsForSamePassword() {
        String rawPassword = "mySecretPassword123";
        String hash1 = encoder.encode(rawPassword);
        String hash2 = encoder.encode(rawPassword);

        assertThat(hash1).isNotEqualTo(hash2);
        assertThat(encoder.matches(rawPassword, hash1)).isTrue();
        assertThat(encoder.matches(rawPassword, hash2)).isTrue();
    }

    @Test
    void shouldReturnFalseForInvalidHashFormatOrNull() {
        assertThat(encoder.matches("password", null)).isFalse();
        assertThat(encoder.matches(null, "someHash")).isFalse();
        assertThat(encoder.matches("password", "invalid_hash_string")).isFalse();
    }
}
