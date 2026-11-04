package dev.ordago.shared.infrastructure.ratelimit;

import java.time.Duration;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Manages in-memory token buckets per client IP and tier using Bucket4j backed by a Caffeine cache.
 */
@ApplicationScoped
public class RateLimitService {

    @ConfigProperty(name = "ordago.rate-limit.enabled", defaultValue = "true")
    boolean enabled;

    @ConfigProperty(name = "ordago.rate-limit.login.capacity", defaultValue = "5")
    long loginCapacity;

    @ConfigProperty(name = "ordago.rate-limit.login.duration-minutes", defaultValue = "1")
    int loginDurationMinutes;

    @ConfigProperty(name = "ordago.rate-limit.register.capacity", defaultValue = "3")
    long registerCapacity;

    @ConfigProperty(name = "ordago.rate-limit.register.duration-minutes", defaultValue = "1")
    int registerDurationMinutes;

    @ConfigProperty(name = "ordago.rate-limit.password-reset.capacity", defaultValue = "3")
    long passwordResetCapacity;

    @ConfigProperty(name = "ordago.rate-limit.password-reset.duration-minutes", defaultValue = "1")
    int passwordResetDurationMinutes;

    @ConfigProperty(name = "ordago.rate-limit.resend-verification.capacity", defaultValue = "3")
    long resendVerificationCapacity;

    @ConfigProperty(name = "ordago.rate-limit.resend-verification.duration-minutes", defaultValue = "1")
    int resendVerificationDurationMinutes;

    @ConfigProperty(name = "ordago.rate-limit.refresh.capacity", defaultValue = "10")
    long refreshCapacity;

    @ConfigProperty(name = "ordago.rate-limit.refresh.duration-minutes", defaultValue = "1")
    int refreshDurationMinutes;

    @ConfigProperty(name = "ordago.rate-limit.standard.capacity", defaultValue = "60")
    long standardCapacity;

    @ConfigProperty(name = "ordago.rate-limit.standard.duration-minutes", defaultValue = "1")
    int standardDurationMinutes;

    private final Cache<String, Bucket> bucketCache;

    public RateLimitService() {
        this.bucketCache = Caffeine.newBuilder()
                .expireAfterAccess(Duration.ofMinutes(15))
                .maximumSize(100_000)
                .build();
    }

    public RateLimitService(
            boolean enabled,
            long loginCapacity, int loginDurationMinutes,
            long registerCapacity, int registerDurationMinutes,
            long passwordResetCapacity, int passwordResetDurationMinutes,
            long resendVerificationCapacity, int resendVerificationDurationMinutes,
            long refreshCapacity, int refreshDurationMinutes,
            long standardCapacity, int standardDurationMinutes
    ) {
        this();
        this.enabled = enabled;
        this.loginCapacity = loginCapacity;
        this.loginDurationMinutes = loginDurationMinutes;
        this.registerCapacity = registerCapacity;
        this.registerDurationMinutes = registerDurationMinutes;
        this.passwordResetCapacity = passwordResetCapacity;
        this.passwordResetDurationMinutes = passwordResetDurationMinutes;
        this.resendVerificationCapacity = resendVerificationCapacity;
        this.resendVerificationDurationMinutes = resendVerificationDurationMinutes;
        this.refreshCapacity = refreshCapacity;
        this.refreshDurationMinutes = refreshDurationMinutes;
        this.standardCapacity = standardCapacity;
        this.standardDurationMinutes = standardDurationMinutes;
    }

    public static RateLimitService createWithDefaults(boolean enabled) {
        return new RateLimitService(
                enabled,
                RateLimitTier.LOGIN.getDefaultCapacity(), RateLimitTier.LOGIN.getDefaultDurationMinutes(),
                RateLimitTier.REGISTER.getDefaultCapacity(), RateLimitTier.REGISTER.getDefaultDurationMinutes(),
                RateLimitTier.PASSWORD_RESET.getDefaultCapacity(), RateLimitTier.PASSWORD_RESET.getDefaultDurationMinutes(),
                RateLimitTier.RESEND_VERIFICATION.getDefaultCapacity(), RateLimitTier.RESEND_VERIFICATION.getDefaultDurationMinutes(),
                RateLimitTier.REFRESH.getDefaultCapacity(), RateLimitTier.REFRESH.getDefaultDurationMinutes(),
                RateLimitTier.STANDARD.getDefaultCapacity(), RateLimitTier.STANDARD.getDefaultDurationMinutes()
        );
    }

    public ConsumptionProbe consumeToken(String clientIp, RateLimitTier tier) {
        RateLimitTier effectiveTier = (tier != null) ? tier : RateLimitTier.STANDARD;
        long capacity = getCapacity(effectiveTier);

        if (!enabled) {
            return ConsumptionProbe.consumed(capacity, 0L);
        }

        String safeIp = (clientIp != null && !clientIp.isBlank()) ? clientIp : "127.0.0.1";
        String key = safeIp + ":" + effectiveTier.name();
        Bucket bucket = bucketCache.get(key, k -> createBucket(effectiveTier));
        return bucket.tryConsumeAndReturnRemaining(1);
    }

    private Bucket createBucket(RateLimitTier tier) {
        long capacity = getCapacity(tier);
        int durationMinutes = getDurationMinutes(tier);
        Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillIntervally(capacity, Duration.ofMinutes(durationMinutes))
                .build();
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    public long getCapacity(RateLimitTier tier) {
        if (tier == null) {
            return standardCapacity;
        }
        return switch (tier) {
            case LOGIN -> loginCapacity;
            case REGISTER -> registerCapacity;
            case PASSWORD_RESET -> passwordResetCapacity;
            case RESEND_VERIFICATION -> resendVerificationCapacity;
            case REFRESH -> refreshCapacity;
            case STANDARD -> standardCapacity;
        };
    }

    public int getDurationMinutes(RateLimitTier tier) {
        if (tier == null) {
            return standardDurationMinutes;
        }
        return switch (tier) {
            case LOGIN -> loginDurationMinutes;
            case REGISTER -> registerDurationMinutes;
            case PASSWORD_RESET -> passwordResetDurationMinutes;
            case RESEND_VERIFICATION -> resendVerificationDurationMinutes;
            case REFRESH -> refreshDurationMinutes;
            case STANDARD -> standardDurationMinutes;
        };
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void clearCache() {
        bucketCache.invalidateAll();
    }

    public long cacheSize() {
        return bucketCache.estimatedSize();
    }
}
