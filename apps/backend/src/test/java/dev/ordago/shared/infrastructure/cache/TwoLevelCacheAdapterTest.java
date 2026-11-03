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
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.keys.KeyCommands;
import io.quarkus.redis.datasource.value.ValueCommands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TwoLevelCacheAdapter Unit Tests")
class TwoLevelCacheAdapterTest {

    private CacheProperties properties;
    private CaffeineL1Cache l1Cache;
    private RedisL2Cache l2Cache;
    private TwoLevelCacheAdapter adapter;

    private Map<String, String> redisDataStore;
    private Map<String, Long> redisTtlStore;

    record UserProfileCacheDto(String userId, String username, int rank) {}

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        properties = new CacheProperties(1000L, 120L, 3600L, "ordago:cache:");
        l1Cache = new CaffeineL1Cache(properties);

        redisDataStore = new HashMap<>();
        redisTtlStore = new HashMap<>();

        ValueCommands<String, String> valueCommands = (ValueCommands<String, String>) Proxy.newProxyInstance(
                ValueCommands.class.getClassLoader(),
                new Class<?>[]{ValueCommands.class},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) {
                        String name = method.getName();
                        if ("get".equals(name)) {
                            return redisDataStore.get(args[0]);
                        }
                        if ("setex".equals(name)) {
                            redisDataStore.put((String) args[0], (String) args[2]);
                            redisTtlStore.put((String) args[0], (Long) args[1]);
                            return null;
                        }
                        if ("set".equals(name)) {
                            redisDataStore.put((String) args[0], (String) args[1]);
                            return null;
                        }
                        return null;
                    }
                }
        );

        KeyCommands<String> keyCommands = (KeyCommands<String>) Proxy.newProxyInstance(
                KeyCommands.class.getClassLoader(),
                new Class<?>[]{KeyCommands.class},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) {
                        String name = method.getName();
                        if ("del".equals(name)) {
                            if (args[0] instanceof String key) {
                                return redisDataStore.remove(key) != null ? 1 : 0;
                            } else if (args[0] instanceof String[] keys) {
                                int count = 0;
                                for (String k : keys) {
                                    if (redisDataStore.remove(k) != null) count++;
                                }
                                return count;
                            }
                            return 0;
                        }
                        if ("keys".equals(name)) {
                            String pattern = (String) args[0];
                            String prefix = pattern.replace("*", "");
                            List<String> matched = new ArrayList<>();
                            for (String k : redisDataStore.keySet()) {
                                if (k.startsWith(prefix)) matched.add(k);
                            }
                            return matched;
                        }
                        if ("exists".equals(name)) {
                            return redisDataStore.containsKey((String) args[0]);
                        }
                        return null;
                    }
                }
        );

        RedisDataSource redisDataSource = (RedisDataSource) Proxy.newProxyInstance(
                RedisDataSource.class.getClassLoader(),
                new Class<?>[]{RedisDataSource.class},
                (proxy, method, args) -> {
                    if ("value".equals(method.getName())) return valueCommands;
                    if ("key".equals(method.getName())) return keyCommands;
                    return null;
                }
        );

        l2Cache = new RedisL2Cache(redisDataSource, new ObjectMapper(), properties);
        adapter = new TwoLevelCacheAdapter(l1Cache, l2Cache, properties);
    }

    @Test
    @DisplayName("Should validate non-null constructor arguments")
    void shouldValidateConstructorArguments() {
        assertThatThrownBy(() -> new TwoLevelCacheAdapter(null, l2Cache, properties))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("l1Cache must not be null");

        assertThatThrownBy(() -> new TwoLevelCacheAdapter(l1Cache, null, properties))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("l2Cache must not be null");

        assertThatThrownBy(() -> new TwoLevelCacheAdapter(l1Cache, l2Cache, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("cacheProperties must not be null");
    }

    @Test
    @DisplayName("get() should return value from L1 when hit without querying L2")
    void shouldReturnL1HitWithoutHittingL2() {
        l1Cache.put("key:l1", "L1Value");

        Optional<String> result = adapter.get("key:l1", String.class);

        assertThat(result).isPresent().contains("L1Value");
        assertThat(redisDataStore).doesNotContainKey("ordago:cache:key:l1");
    }

    @Test
    @DisplayName("get() on L1 miss and L2 hit should return L2 value and backfill L1")
    void shouldBackfillL1OnL2Hit() {
        UserProfileCacheDto profile = new UserProfileCacheDto("u1", "MusChamp", 1500);
        l2Cache.put("profile:u1", profile);

        assertThat(l1Cache.exists("profile:u1")).isFalse();

        Optional<UserProfileCacheDto> result = adapter.get("profile:u1", UserProfileCacheDto.class);

        assertThat(result).isPresent().contains(profile);
        assertThat(l1Cache.exists("profile:u1")).isTrue();
        assertThat(l1Cache.get("profile:u1", UserProfileCacheDto.class)).contains(profile);
    }

    @Test
    @DisplayName("get() on L1 miss and L2 miss should return empty Optional")
    void shouldReturnEmptyOnBothMiss() {
        Optional<String> result = adapter.get("missingKey", String.class);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getOrCompute() should return L1 value when present without invoking supplier")
    void shouldReturnL1ValueWithoutInvokingSupplier() {
        l1Cache.put("stat:1", 99);
        AtomicInteger supplierCalls = new AtomicInteger(0);

        Integer value = adapter.getOrCompute("stat:1", Integer.class, () -> {
            supplierCalls.incrementAndGet();
            return 100;
        });

        assertThat(value).isEqualTo(99);
        assertThat(supplierCalls.get()).isZero();
    }

    @Test
    @DisplayName("getOrCompute() should return L2 value on L1 miss, backfill L1 and not invoke supplier")
    void shouldReturnL2ValueAndNotInvokeSupplier() {
        l2Cache.put("stat:2", 200);
        AtomicInteger supplierCalls = new AtomicInteger(0);

        Integer value = adapter.getOrCompute("stat:2", Integer.class, () -> {
            supplierCalls.incrementAndGet();
            return 999;
        }, Duration.ofMinutes(5), Duration.ofHours(1));

        assertThat(value).isEqualTo(200);
        assertThat(supplierCalls.get()).isZero();
        assertThat(l1Cache.get("stat:2", Integer.class)).contains(200);
    }

    @Test
    @DisplayName("getOrCompute() on miss should compute value, store in both L1 and L2 and return it")
    void shouldComputeAndStoreInBothTiersOnMiss() {
        AtomicInteger supplierCalls = new AtomicInteger(0);

        String computed = adapter.getOrCompute("config:hero", String.class, () -> {
            supplierCalls.incrementAndGet();
            return "computedValue";
        }, Duration.ofSeconds(30), Duration.ofSeconds(300));

        assertThat(computed).isEqualTo("computedValue");
        assertThat(supplierCalls.get()).isEqualTo(1);
        assertThat(l1Cache.get("config:hero", String.class)).contains("computedValue");
        assertThat(l2Cache.get("config:hero", String.class)).contains("computedValue");
        assertThat(redisTtlStore.get("ordago:cache:config:hero")).isEqualTo(300L);
    }

    @Test
    @DisplayName("getOrCompute() with default TTLs should populate both tiers")
    void shouldComputeWithDefaultTtls() {
        AtomicInteger supplierCalls = new AtomicInteger(0);

        String computed = adapter.getOrCompute("default:key", String.class, () -> {
            supplierCalls.incrementAndGet();
            return "hello";
        });

        assertThat(computed).isEqualTo("hello");
        assertThat(supplierCalls.get()).isEqualTo(1);
        assertThat(l1Cache.exists("default:key")).isTrue();
        assertThat(l2Cache.exists("default:key")).isTrue();
    }

    @Test
    @DisplayName("getOrCompute() should reject null supplier with NullPointerException")
    void shouldRejectNullSupplier() {
        assertThatThrownBy(() -> adapter.getOrCompute("key", String.class, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("supplier must not be null");
    }

    @Test
    @DisplayName("getOrCompute() should handle null computed value gracefully")
    void shouldHandleNullComputedValue() {
        String result = adapter.getOrCompute("nullKey", String.class, () -> null);

        assertThat(result).isNull();
        assertThat(l1Cache.exists("nullKey")).isFalse();
        assertThat(l2Cache.exists("nullKey")).isFalse();
    }

    @Test
    @DisplayName("put() should store value into both L1 and L2 with specified TTLs")
    void shouldPutInBothTiersWithExplicitTtls() {
        adapter.put("manualKey", "manualVal", Duration.ofSeconds(10), Duration.ofSeconds(60));

        assertThat(l1Cache.get("manualKey", String.class)).contains("manualVal");
        assertThat(l2Cache.get("manualKey", String.class)).contains("manualVal");
        assertThat(redisTtlStore.get("ordago:cache:manualKey")).isEqualTo(60L);
    }

    @Test
    @DisplayName("put() with default TTLs should store value in both tiers")
    void shouldPutInBothTiersWithDefaultTtls() {
        adapter.put("manualDefault", "val");

        assertThat(l1Cache.get("manualDefault", String.class)).contains("val");
        assertThat(l2Cache.get("manualDefault", String.class)).contains("val");
        assertThat(redisTtlStore.get("ordago:cache:manualDefault")).isEqualTo(3600L);
    }

    @Test
    @DisplayName("evict() should evict entry from both L1 and L2")
    void shouldEvictFromBothTiers() {
        adapter.put("toEvict", "data");
        assertThat(adapter.exists("toEvict")).isTrue();

        adapter.evict("toEvict");

        assertThat(l1Cache.exists("toEvict")).isFalse();
        assertThat(l2Cache.exists("toEvict")).isFalse();
        assertThat(adapter.exists("toEvict")).isFalse();
    }

    @Test
    @DisplayName("evictAll() should clear both L1 and L2")
    void shouldEvictAllFromBothTiers() {
        adapter.put("a", "1");
        adapter.put("b", "2");

        adapter.evictAll();

        assertThat(adapter.exists("a")).isFalse();
        assertThat(adapter.exists("b")).isFalse();
    }

    @Test
    @DisplayName("exists() should return true if key is in L1 or L2")
    void shouldCheckExistsAcrossTiers() {
        assertThat(adapter.exists("any")).isFalse();

        l2Cache.put("onlyL2", "data");
        assertThat(adapter.exists("onlyL2")).isTrue();

        l1Cache.put("onlyL1", "data");
        assertThat(adapter.exists("onlyL1")).isTrue();
    }
}
