package dev.ordago.shared.infrastructure.cache;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.keys.KeyCommands;
import io.quarkus.redis.datasource.value.ValueCommands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RedisL2Cache Unit Tests")
class RedisL2CacheTest {

    private CacheProperties properties;
    private ObjectMapper objectMapper;
    private Map<String, String> dataStore;
    private Map<String, Long> ttlStore;
    private FakeValueInvocationHandler valueHandler;
    private FakeKeyInvocationHandler keyHandler;
    private ValueCommands<String, String> valueCommands;
    private KeyCommands<String> keyCommands;
    private RedisDataSource redisDataSource;
    private RedisL2Cache cache;

    record TestPayload(String id, String content, int count) {}

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        properties = new CacheProperties(5000L, 60L, 1800L, "ordago:cache:");
        objectMapper = new ObjectMapper();
        dataStore = new HashMap<>();
        ttlStore = new HashMap<>();

        valueHandler = new FakeValueInvocationHandler(dataStore, ttlStore);
        keyHandler = new FakeKeyInvocationHandler(dataStore);

        valueCommands = (ValueCommands<String, String>) Proxy.newProxyInstance(
                ValueCommands.class.getClassLoader(),
                new Class<?>[]{ValueCommands.class},
                valueHandler
        );

        keyCommands = (KeyCommands<String>) Proxy.newProxyInstance(
                KeyCommands.class.getClassLoader(),
                new Class<?>[]{KeyCommands.class},
                keyHandler
        );

        redisDataSource = (RedisDataSource) Proxy.newProxyInstance(
                RedisDataSource.class.getClassLoader(),
                new Class<?>[]{RedisDataSource.class},
                (proxy, method, args) -> {
                    if ("value".equals(method.getName())) {
                        return valueCommands;
                    }
                    if ("key".equals(method.getName())) {
                        return keyCommands;
                    }
                    return null;
                }
        );

        cache = new RedisL2Cache(redisDataSource, objectMapper, properties);
    }

    @Test
    @DisplayName("Constructor should validate null dependencies")
    void shouldValidateConstructorArguments() {
        assertThatThrownBy(() -> new RedisL2Cache(null, objectMapper, properties))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("redisDataSource must not be null");

        assertThatThrownBy(() -> new RedisL2Cache(redisDataSource, null, properties))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("objectMapper must not be null");

        assertThatThrownBy(() -> new RedisL2Cache(redisDataSource, objectMapper, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("cacheProperties must not be null");

        assertThatThrownBy(() -> new RedisL2Cache(null, keyCommands, objectMapper, properties))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("valueCommands must not be null");

        assertThatThrownBy(() -> new RedisL2Cache(valueCommands, null, objectMapper, properties))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("keyCommands must not be null");
    }

    @Test
    @DisplayName("Should put and get entry with default TTL using JSON serialization")
    void shouldPutAndGetWithDefaultTtl() {
        TestPayload payload = new TestPayload("p1", "Sample", 10);
        cache.put("payload:1", payload);

        String expectedRedisKey = "ordago:cache:payload:1";
        assertThat(dataStore).containsKey(expectedRedisKey);
        assertThat(ttlStore.get(expectedRedisKey)).isEqualTo(1800L);

        Optional<TestPayload> retrieved = cache.get("payload:1", TestPayload.class);
        assertThat(retrieved).isPresent().contains(payload);
    }

    @Test
    @DisplayName("Should put entry with explicit custom TTL")
    void shouldPutWithCustomTtl() {
        cache.put("custom:1", "hello", Duration.ofSeconds(45));

        String expectedRedisKey = "ordago:cache:custom:1";
        assertThat(ttlStore.get(expectedRedisKey)).isEqualTo(45L);
        assertThat(cache.get("custom:1", String.class)).isPresent().contains("hello");
    }

    @Test
    @DisplayName("Should fallback to default TTL when provided TTL is zero or negative")
    void shouldFallbackToDefaultTtlWhenZeroOrNegative() {
        cache.put("fallback-zero:1", "val1", Duration.ZERO);
        cache.put("fallback-neg:1", "val2", Duration.ofSeconds(-10));

        String keyZero = "ordago:cache:fallback-zero:1";
        String keyNeg = "ordago:cache:fallback-neg:1";

        assertThat(dataStore).containsKey(keyZero);
        assertThat(dataStore).containsKey(keyNeg);
        assertThat(ttlStore.get(keyZero)).isEqualTo(1800L);
        assertThat(ttlStore.get(keyNeg)).isEqualTo(1800L);
    }

    @Test
    @DisplayName("Should return empty Optional when key is absent")
    void shouldReturnEmptyWhenKeyAbsent() {
        Optional<String> result = cache.get("missing", String.class);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return empty Optional when key or type is null")
    void shouldReturnEmptyWhenKeyOrTypeNull() {
        cache.put("k1", "v1");
        assertThat(cache.get(null, String.class)).isEmpty();
        assertThat(cache.get("k1", null)).isEmpty();
    }

    @Test
    @DisplayName("Should handle null key or null value in put without throwing")
    void shouldHandleNullKeyOrValueInPut() {
        assertThatCode(() -> cache.put(null, "val")).doesNotThrowAnyException();
        assertThatCode(() -> cache.put("key", null)).doesNotThrowAnyException();
        assertThat(dataStore).isEmpty();
    }

    @Test
    @DisplayName("Should evict entry by key")
    void shouldEvictEntry() {
        cache.put("item:1", "data");
        assertThat(cache.exists("item:1")).isTrue();

        cache.evict("item:1");

        assertThat(cache.exists("item:1")).isFalse();
        assertThat(cache.get("item:1", String.class)).isEmpty();
    }

    @Test
    @DisplayName("Should handle evict with null key gracefully")
    void shouldHandleEvictNullKey() {
        assertThatCode(() -> cache.evict(null)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should evict all entries matching prefix on evictAll()")
    void shouldEvictAllEntries() {
        cache.put("a", "1");
        cache.put("b", "2");
        dataStore.put("unrelated:key", "keep");

        cache.evictAll();

        assertThat(cache.exists("a")).isFalse();
        assertThat(cache.exists("b")).isFalse();
        assertThat(dataStore).containsKey("unrelated:key");
    }

    @Test
    @DisplayName("Should check existence correctly including null handling")
    void shouldCheckExistence() {
        assertThat(cache.exists(null)).isFalse();
        assertThat(cache.exists("someKey")).isFalse();

        cache.put("someKey", "val");
        assertThat(cache.exists("someKey")).isTrue();
    }

    @Test
    @DisplayName("Should fail gracefully and return empty Optional when Redis get throws exception")
    void shouldFailGracefullyOnGetException() {
        cache.put("faultyKey", "validData");
        valueHandler.setGetException(new RuntimeException("Redis connection timed out"));

        Optional<String> result = cache.get("faultyKey", String.class);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should fail gracefully when stored value is malformed JSON")
    void shouldFailGracefullyOnMalformedJson() {
        dataStore.put("ordago:cache:badJson", "{ invalid json content ... }");

        Optional<TestPayload> result = cache.get("badJson", TestPayload.class);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should fail gracefully without throwing when Redis put throws exception")
    void shouldFailGracefullyOnPutException() {
        valueHandler.setPutException(new RuntimeException("Redis read-only replica"));

        assertThatCode(() -> cache.put("failKey", "val")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should fail gracefully without throwing when Redis evict throws exception")
    void shouldFailGracefullyOnEvictException() {
        keyHandler.setDelException(new RuntimeException("Redis cluster rebalancing"));

        assertThatCode(() -> cache.evict("anyKey")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should fail gracefully without throwing when Redis evictAll throws exception")
    void shouldFailGracefullyOnEvictAllException() {
        keyHandler.setKeysException(new RuntimeException("Redis OOM command disabled"));

        assertThatCode(() -> cache.evictAll()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should fail gracefully and return false when Redis exists throws exception")
    void shouldFailGracefullyOnExistsException() {
        keyHandler.setExistsException(new RuntimeException("Redis connection refused"));

        assertThat(cache.exists("anyKey")).isFalse();
    }

    private static class FakeValueInvocationHandler implements InvocationHandler {
        private final Map<String, String> data;
        private final Map<String, Long> ttls;
        private RuntimeException getException;
        private RuntimeException putException;

        FakeValueInvocationHandler(Map<String, String> data, Map<String, Long> ttls) {
            this.data = data;
            this.ttls = ttls;
        }

        void setGetException(RuntimeException ex) {
            this.getException = ex;
        }

        void setPutException(RuntimeException ex) {
            this.putException = ex;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();
            if ("get".equals(name)) {
                if (getException != null) {
                    throw getException;
                }
                return data.get(args[0]);
            }
            if ("setex".equals(name)) {
                if (putException != null) {
                    throw putException;
                }
                data.put((String) args[0], (String) args[2]);
                ttls.put((String) args[0], (Long) args[1]);
                return null;
            }
            if ("set".equals(name)) {
                if (putException != null) {
                    throw putException;
                }
                data.put((String) args[0], (String) args[1]);
                return null;
            }
            return null;
        }
    }

    private static class FakeKeyInvocationHandler implements InvocationHandler {
        private final Map<String, String> data;
        private RuntimeException delException;
        private RuntimeException keysException;
        private RuntimeException existsException;

        FakeKeyInvocationHandler(Map<String, String> data) {
            this.data = data;
        }

        void setDelException(RuntimeException ex) {
            this.delException = ex;
        }

        void setKeysException(RuntimeException ex) {
            this.keysException = ex;
        }

        void setExistsException(RuntimeException ex) {
            this.existsException = ex;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();
            if ("del".equals(name)) {
                if (delException != null) {
                    throw delException;
                }
                if (args[0] instanceof String key) {
                    return data.remove(key) != null ? 1 : 0;
                } else if (args[0] instanceof String[] keys) {
                    int removed = 0;
                    for (String k : keys) {
                        if (data.remove(k) != null) {
                            removed++;
                        }
                    }
                    return removed;
                }
                return 0;
            }
            if ("keys".equals(name)) {
                if (keysException != null) {
                    throw keysException;
                }
                String pattern = (String) args[0];
                String prefix = pattern.replace("*", "");
                List<String> matched = new ArrayList<>();
                for (String k : data.keySet()) {
                    if (k.startsWith(prefix)) {
                        matched.add(k);
                    }
                }
                return matched;
            }
            if ("exists".equals(name)) {
                if (existsException != null) {
                    throw existsException;
                }
                String key = (String) args[0];
                return data.containsKey(key);
            }
            return null;
        }
    }
}
