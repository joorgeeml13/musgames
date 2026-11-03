package dev.ordago.shared.infrastructure.cache;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.jboss.logging.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.keys.KeyCommands;
import io.quarkus.redis.datasource.value.ValueCommands;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * L2 distributed cache implementation backed by Redis.
 * Uses JSON serialization via Jackson and provides fault-tolerant degradation on connectivity issues.
 */
@ApplicationScoped
public class RedisL2Cache {

    private static final Logger LOG = Logger.getLogger(RedisL2Cache.class);

    private final ValueCommands<String, String> valueCommands;
    private final KeyCommands<String> keyCommands;
    private final ObjectMapper objectMapper;
    private final CacheProperties cacheProperties;

    @Inject
    public RedisL2Cache(
            RedisDataSource redisDataSource,
            ObjectMapper objectMapper,
            CacheProperties cacheProperties
    ) {
        Objects.requireNonNull(redisDataSource, "redisDataSource must not be null");
        this.valueCommands = redisDataSource.value(String.class);
        this.keyCommands = redisDataSource.key();
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.cacheProperties = Objects.requireNonNull(cacheProperties, "cacheProperties must not be null");
    }

    RedisL2Cache(
            ValueCommands<String, String> valueCommands,
            KeyCommands<String> keyCommands,
            ObjectMapper objectMapper,
            CacheProperties cacheProperties
    ) {
        this.valueCommands = Objects.requireNonNull(valueCommands, "valueCommands must not be null");
        this.keyCommands = Objects.requireNonNull(keyCommands, "keyCommands must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.cacheProperties = Objects.requireNonNull(cacheProperties, "cacheProperties must not be null");
    }

    public <T> Optional<T> get(String key, Class<T> type) {
        if (key == null || type == null) {
            return Optional.empty();
        }

        try {
            String redisKey = buildRedisKey(key);
            String json = valueCommands.get(redisKey);
            if (json == null) {
                return Optional.empty();
            }

            T value = objectMapper.readValue(json, type);
            return Optional.ofNullable(value);
        } catch (Exception e) {
            LOG.warnf(e, "Redis L2 cache get failed for key '%s'", key);
            return Optional.empty();
        }
    }

    public void put(String key, Object value, Duration ttl) {
        if (key == null) {
            LOG.warn("Cannot put entry with null key into L2 cache");
            return;
        }
        if (value == null) {
            LOG.warnf("Cannot put null value for key '%s' into L2 cache", key);
            return;
        }

        try {
            String redisKey = buildRedisKey(key);
            String json = objectMapper.writeValueAsString(value);

            Duration effectiveTtl = (ttl != null && !ttl.isNegative() && !ttl.isZero())
                    ? ttl
                    : cacheProperties.getL2DefaultTtl();

            long ttlSeconds = effectiveTtl.toSeconds();
            if (ttlSeconds > 0) {
                valueCommands.setex(redisKey, ttlSeconds, json);
            } else {
                valueCommands.set(redisKey, json);
            }
        } catch (Exception e) {
            LOG.warnf(e, "Redis L2 cache put failed for key '%s'", key);
        }
    }

    public void put(String key, Object value) {
        put(key, value, cacheProperties.getL2DefaultTtl());
    }

    public void evict(String key) {
        if (key == null) {
            return;
        }

        try {
            String redisKey = buildRedisKey(key);
            keyCommands.del(redisKey);
        } catch (Exception e) {
            LOG.warnf(e, "Redis L2 cache evict failed for key '%s'", key);
        }
    }

    public void evictAll() {
        try {
            String pattern = cacheProperties.getL2KeyPrefix() + "*";
            List<String> keys = keyCommands.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                keyCommands.del(keys.toArray(String[]::new));
            }
        } catch (Exception e) {
            LOG.warnf(e, "Redis L2 cache evictAll failed");
        }
    }

    public boolean exists(String key) {
        if (key == null) {
            return false;
        }

        try {
            String redisKey = buildRedisKey(key);
            return keyCommands.exists(redisKey);
        } catch (Exception e) {
            LOG.warnf(e, "Redis L2 cache exists check failed for key '%s'", key);
            return false;
        }
    }

    private String buildRedisKey(String key) {
        return cacheProperties.getL2KeyPrefix() + key;
    }
}
