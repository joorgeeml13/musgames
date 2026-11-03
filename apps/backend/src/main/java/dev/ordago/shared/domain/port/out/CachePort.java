package dev.ordago.shared.domain.port.out;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Driven port for caching operations across L1 (in-memory) and L2 (distributed) tiers.
 * Pure domain interface decoupled from caching libraries and framework annotations.
 */
public interface CachePort {

    /**
     * Retrieves an entry from cache if present.
     *
     * @param key the cache key
     * @param type the expected value type
     * @param <T> the type parameter
     * @return an Optional containing the cached value, or empty if not found
     */
    <T> Optional<T> get(String key, Class<T> type);

    /**
     * Retrieves an entry or computes it via the provided supplier with explicit TTLs.
     *
     * @param key the cache key
     * @param type the expected value type
     * @param supplier the fallback computation supplier
     * @param l1Ttl time-to-live for L1 cache (null uses default)
     * @param l2Ttl time-to-live for L2 cache (null uses default)
     * @param <T> the type parameter
     * @return the cached or computed value
     */
    <T> T getOrCompute(String key, Class<T> type, Supplier<T> supplier, Duration l1Ttl, Duration l2Ttl);

    /**
     * Retrieves an entry or computes it via the provided supplier using default TTLs.
     *
     * @param key the cache key
     * @param type the expected value type
     * @param supplier the fallback computation supplier
     * @param <T> the type parameter
     * @return the cached or computed value
     */
    <T> T getOrCompute(String key, Class<T> type, Supplier<T> supplier);

    /**
     * Stores an entry in both cache tiers with explicit TTLs.
     *
     * @param key the cache key
     * @param value the value to cache
     * @param l1Ttl time-to-live for L1 cache (null uses default)
     * @param l2Ttl time-to-live for L2 cache (null uses default)
     */
    void put(String key, Object value, Duration l1Ttl, Duration l2Ttl);

    /**
     * Stores an entry in both cache tiers using default TTLs.
     *
     * @param key the cache key
     * @param value the value to cache
     */
    void put(String key, Object value);

    /**
     * Evicts an entry by key from all cache tiers.
     *
     * @param key the cache key
     */
    void evict(String key);

    /**
     * Evicts all entries managed under the cache namespace across all tiers.
     */
    void evictAll();

    /**
     * Checks if a key exists in any cache tier.
     *
     * @param key the cache key
     * @return true if the key exists in L1 or L2, false otherwise
     */
    boolean exists(String key);
}
