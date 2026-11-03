package dev.ordago.shared.infrastructure.cache;

import java.time.Duration;
import java.util.Objects;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Configuration bean binding two-level cache properties from application configuration.
 */
@ApplicationScoped
public class CacheProperties {

    private final long l1MaxSize;
    private final Duration l1DefaultTtl;
    private final Duration l2DefaultTtl;
    private final String l2KeyPrefix;

    @Inject
    public CacheProperties(
            @ConfigProperty(name = "ordago.cache.l1.max-size", defaultValue = "10000") long l1MaxSize,
            @ConfigProperty(name = "ordago.cache.l1.default-ttl-seconds", defaultValue = "300") long l1DefaultTtlSeconds,
            @ConfigProperty(name = "ordago.cache.l2.default-ttl-seconds", defaultValue = "3600") long l2DefaultTtlSeconds,
            @ConfigProperty(name = "ordago.cache.l2.key-prefix", defaultValue = "ordago:cache:") String l2KeyPrefix
    ) {
        this.l1MaxSize = l1MaxSize;
        this.l1DefaultTtl = Duration.ofSeconds(l1DefaultTtlSeconds);
        this.l2DefaultTtl = Duration.ofSeconds(l2DefaultTtlSeconds);
        this.l2KeyPrefix = Objects.requireNonNull(l2KeyPrefix, "l2KeyPrefix must not be null");
    }

    public long getL1MaxSize() {
        return l1MaxSize;
    }

    public Duration getL1DefaultTtl() {
        return l1DefaultTtl;
    }

    public Duration getL2DefaultTtl() {
        return l2DefaultTtl;
    }

    public String getL2KeyPrefix() {
        return l2KeyPrefix;
    }
}
