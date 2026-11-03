package dev.ordago.shared.infrastructure.cache;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import org.jboss.logging.Logger;
import dev.ordago.shared.domain.port.out.CachePort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Two-level cache adapter implementing {@link CachePort}.
 * Coordinates microsecond L1 Caffeine access with shared L2 Redis persistence.
 */
@ApplicationScoped
public class TwoLevelCacheAdapter implements CachePort {

    private static final Logger LOG = Logger.getLogger(TwoLevelCacheAdapter.class);

    private final CaffeineL1Cache l1Cache;
    private final RedisL2Cache l2Cache;
    private final CacheProperties cacheProperties;

    @Inject
    public TwoLevelCacheAdapter(
            CaffeineL1Cache l1Cache,
            RedisL2Cache l2Cache,
            CacheProperties cacheProperties
    ) {
        this.l1Cache = Objects.requireNonNull(l1Cache, "l1Cache must not be null");
        this.l2Cache = Objects.requireNonNull(l2Cache, "l2Cache must not be null");
        this.cacheProperties = Objects.requireNonNull(cacheProperties, "cacheProperties must not be null");
    }

    @Override
    public <T> Optional<T> get(String key, Class<T> type) {
        Optional<T> l1Value = l1Cache.get(key, type);
        if (l1Value.isPresent()) {
            LOG.debugf("Cache L1 hit for key '%s'", key);
            return l1Value;
        }

        Optional<T> l2Value = l2Cache.get(key, type);
        if (l2Value.isPresent()) {
            LOG.debugf("Cache L2 hit for key '%s'; populating L1", key);
            l1Cache.put(key, l2Value.get(), cacheProperties.getL1DefaultTtl());
            return l2Value;
        }

        LOG.debugf("Cache miss for key '%s'", key);
        return Optional.empty();
    }

    @Override
    public <T> T getOrCompute(String key, Class<T> type, Supplier<T> supplier, Duration l1Ttl, Duration l2Ttl) {
        Objects.requireNonNull(supplier, "supplier must not be null");

        Optional<T> l1Value = l1Cache.get(key, type);
        if (l1Value.isPresent()) {
            return l1Value.get();
        }

        Optional<T> l2Value = l2Cache.get(key, type);
        if (l2Value.isPresent()) {
            T value = l2Value.get();
            l1Cache.put(key, value, l1Ttl);
            return value;
        }

        T computed = supplier.get();
        if (computed != null) {
            Duration effectiveL1Ttl = l1Ttl != null ? l1Ttl : cacheProperties.getL1DefaultTtl();
            Duration effectiveL2Ttl = l2Ttl != null ? l2Ttl : cacheProperties.getL2DefaultTtl();
            l2Cache.put(key, computed, effectiveL2Ttl);
            l1Cache.put(key, computed, effectiveL1Ttl);
        }
        return computed;
    }

    @Override
    public <T> T getOrCompute(String key, Class<T> type, Supplier<T> supplier) {
        return getOrCompute(key, type, supplier, cacheProperties.getL1DefaultTtl(), cacheProperties.getL2DefaultTtl());
    }

    @Override
    public void put(String key, Object value, Duration l1Ttl, Duration l2Ttl) {
        Duration effectiveL1Ttl = l1Ttl != null ? l1Ttl : cacheProperties.getL1DefaultTtl();
        Duration effectiveL2Ttl = l2Ttl != null ? l2Ttl : cacheProperties.getL2DefaultTtl();
        l2Cache.put(key, value, effectiveL2Ttl);
        l1Cache.put(key, value, effectiveL1Ttl);
    }

    @Override
    public void put(String key, Object value) {
        put(key, value, cacheProperties.getL1DefaultTtl(), cacheProperties.getL2DefaultTtl());
    }

    @Override
    public void evict(String key) {
        l1Cache.evict(key);
        l2Cache.evict(key);
    }

    @Override
    public void evictAll() {
        l1Cache.clear();
        l2Cache.evictAll();
    }

    @Override
    public boolean exists(String key) {
        if (l1Cache.exists(key)) {
            return true;
        }
        return l2Cache.exists(key);
    }
}
