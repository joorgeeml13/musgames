package dev.ordago.shared.infrastructure.cache;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CacheProperties Configuration Unit Tests")
class CachePropertiesTest {

    @Test
    @DisplayName("Should correctly bind configured cache properties and calculate Durations")
    void shouldBindConfiguredProperties() {
        CacheProperties properties = new CacheProperties(
                5000L,
                120L,
                1800L,
                "custom:prefix:"
        );

        assertThat(properties.getL1MaxSize()).isEqualTo(5000L);
        assertThat(properties.getL1DefaultTtl()).isEqualTo(Duration.ofSeconds(120));
        assertThat(properties.getL2DefaultTtl()).isEqualTo(Duration.ofSeconds(1800));
        assertThat(properties.getL2KeyPrefix()).isEqualTo("custom:prefix:");
    }

    @Test
    @DisplayName("Should reject null key prefix with NullPointerException")
    void shouldRejectNullKeyPrefix() {
        assertThatThrownBy(() -> new CacheProperties(1000L, 60L, 300L, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("l2KeyPrefix must not be null");
    }
}
