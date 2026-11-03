package dev.ordago.shared.infrastructure.cache;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import org.jboss.logging.Logger;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * L1 in-memory cache implementation backed by Caffeine.
 * Provides microsecond-level reads, maximum size boundaries, and per-entry expiration.
 */
@ApplicationScoped
public class CaffeineL1Cache {

    private static final Logger LOG = Logger.getLogger(CaffeineL1Cache.class);

    private final Cache<String, Object> cache;
    private final CacheProperties cacheProperties;

    @Inject
    public CaffeineL1Cache(CacheProperties cacheProperties) {
        this.cacheProperties = Objects.requireNonNull(cacheProperties, "cacheProperties must not be null");
        this.cache = Caffeine.newBuilder()
                .maximumSize(cacheProperties.getL1MaxSize())
                .expireAfter(new Expiry<String, Object>() {
                    @Override
                    public long expireAfterCreate(String key, Object value, long currentTime) {
                        if (value instanceof L1CacheEntry entry) {
                            return entry.ttl().toNanos();
                        }
                        return cacheProperties.getL1DefaultTtl().toNanos();
                    }

                    @Override
                    public long expireAfterUpdate(String key, Object value, long currentTime, long currentDuration) {
                        if (value instanceof L1CacheEntry entry) {
                            return entry.ttl().toNanos();
                        }
                        return cacheProperties.getL1DefaultTtl().toNanos();
                    }

                    @Override
                    public long expireAfterRead(String key, Object value, long currentTime, long currentDuration) {
                        return currentDuration;
                    }
                })
                .build();
    }

    public <T> Optional<T> get(String key, Class<T> type) {
        if (key == null || type == null) {
            return Optional.empty();
        }

        Object raw = cache.getIfPresent(key);
        if (raw == null) {
            return Optional.empty();
        }

        Object value = (raw instanceof L1CacheEntry entry) ? entry.value() : raw;
        if (value == null) {
            return Optional.empty();
        }

        if (type.isInstance(value)) {
            return Optional.of(type.cast(value));
        }

        LOG.warnf("L1 cache key '%s' has value of type %s, expected %s", key, value.getClass().getName(), type.getName());
        return Optional.empty();
    }

    public void put(String key, Object value, Duration ttl) {
        if (key == null) {
            LOG.warn("Cannot put entry with null key into L1 cache");
            return;
        }
        if (value == null) {
            LOG.warnf("Cannot put null value for key '%s' into L1 cache", key);
            return;
        }

        Duration effectiveTtl = (ttl != null && !ttl.isNegative() && !ttl.isZero())
                ? ttl
                : cacheProperties.getL1DefaultTtl();

        cache.put(key, new L1CacheEntry(value, effectiveTtl));
    }

    public void put(String key, Object value) {
        put(key, value, cacheProperties.getL1DefaultTtl());
    }

    public void evict(String key) {
        if (key != null) {
            cache.invalidate(key);
        }
    }

    public void clear() {
        cache.invalidateAll();
    }

    public boolean exists(String key) {
        if (key == null) {
            return false;
        }
        return cache.getIfPresent(key) != null;
    }

    public long size() {
        return cache.estimatedSize();
    }

    private record L1CacheEntry(Object value, Duration ttl) {}
}
