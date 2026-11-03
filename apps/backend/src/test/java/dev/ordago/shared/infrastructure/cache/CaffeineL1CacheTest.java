package dev.ordago.shared.infrastructure.cache;

import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CaffeineL1Cache Unit Tests")
class CaffeineL1CacheTest {

    private CacheProperties properties;
    private CaffeineL1Cache cache;

    @BeforeEach
    void setUp() {
        properties = new CacheProperties(100L, 60L, 300L, "test:cache:");
        cache = new CaffeineL1Cache(properties);
    }

    @Test
    @DisplayName("Should reject null CacheProperties in constructor")
    void shouldRejectNullPropertiesInConstructor() {
        assertThatThrownBy(() -> new CaffeineL1Cache(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("cacheProperties must not be null");
    }

    @Test
    @DisplayName("Should put and retrieve an entry with default TTL")
    void shouldPutAndRetrieveEntryWithDefaultTtl() {
        cache.put("user:123", "UserValue");

        Optional<String> result = cache.get("user:123", String.class);
        assertThat(result).isPresent().contains("UserValue");
        assertThat(cache.exists("user:123")).isTrue();
        assertThat(cache.size()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should put and retrieve an entry with custom TTL")
    void shouldPutAndRetrieveEntryWithCustomTtl() {
        cache.put("session:abc", 42, Duration.ofMinutes(10));

        Optional<Integer> result = cache.get("session:abc", Integer.class);
        assertThat(result).isPresent().contains(42);
        assertThat(cache.exists("session:abc")).isTrue();
    }

    @Test
    @DisplayName("Should fallback to default TTL if provided custom TTL is zero or negative")
    void shouldFallbackToDefaultTtlWhenTtlIsNonPositive() {
        cache.put("key:zero", "zeroVal", Duration.ZERO);
        cache.put("key:negative", "negVal", Duration.ofSeconds(-10));

        assertThat(cache.get("key:zero", String.class)).isPresent().contains("zeroVal");
        assertThat(cache.get("key:negative", String.class)).isPresent().contains("negVal");
    }

    @Test
    @DisplayName("Should return empty Optional when key is absent")
    void shouldReturnEmptyWhenKeyIsAbsent() {
        Optional<String> result = cache.get("nonexistent", String.class);
        assertThat(result).isEmpty();
        assertThat(cache.exists("nonexistent")).isFalse();
    }

    @Test
    @DisplayName("Should return empty Optional when key or type is null")
    void shouldReturnEmptyWhenKeyOrTypeIsNull() {
        cache.put("validKey", "validValue");

        assertThat(cache.get(null, String.class)).isEmpty();
        assertThat(cache.get("validKey", null)).isEmpty();
    }

    @Test
    @DisplayName("Should return empty Optional when cached value type does not match requested type")
    void shouldReturnEmptyWhenTypeMismatches() {
        cache.put("stringKey", "Just a string");

        Optional<Integer> result = cache.get("stringKey", Integer.class);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should handle null key or null value in put gracefully without throwing")
    void shouldHandleNullKeyOrValueInPutGracefully() {
        assertThatCode(() -> cache.put(null, "someValue")).doesNotThrowAnyException();
        assertThatCode(() -> cache.put("someKey", null)).doesNotThrowAnyException();
        assertThatCode(() -> cache.put(null, null, Duration.ofMinutes(1))).doesNotThrowAnyException();
        assertThat(cache.size()).isZero();
    }

    @Test
    @DisplayName("Should evict existing entry by key")
    void shouldEvictExistingEntry() {
        cache.put("temp:1", "tempData");
        assertThat(cache.exists("temp:1")).isTrue();

        cache.evict("temp:1");

        assertThat(cache.exists("temp:1")).isFalse();
        assertThat(cache.get("temp:1", String.class)).isEmpty();
    }

    @Test
    @DisplayName("Should handle evict with null key gracefully")
    void shouldHandleEvictNullKeyGracefully() {
        assertThatCode(() -> cache.evict(null)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should clear all entries on clear()")
    void shouldClearAllEntries() {
        cache.put("k1", "v1");
        cache.put("k2", "v2");
        cache.put("k3", "v3");

        cache.clear();

        assertThat(cache.size()).isZero();
        assertThat(cache.exists("k1")).isFalse();
        assertThat(cache.exists("k2")).isFalse();
        assertThat(cache.exists("k3")).isFalse();
    }

    @Test
    @DisplayName("Should return false when checking existence of null key")
    void shouldReturnFalseForNullKeyExistence() {
        assertThat(cache.exists(null)).isFalse();
    }
}
