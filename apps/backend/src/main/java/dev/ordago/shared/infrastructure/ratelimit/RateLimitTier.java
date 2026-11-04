package dev.ordago.shared.infrastructure.ratelimit;

/**
 * Defines rate limiting tiers and protection levels for critical API endpoints.
 */
public enum RateLimitTier {
    LOGIN(5, 1),
    REGISTER(3, 1),
    PASSWORD_RESET(3, 1),
    RESEND_VERIFICATION(3, 1),
    REFRESH(10, 1),
    STANDARD(60, 1);

    private final long defaultCapacity;
    private final int defaultDurationMinutes;

    RateLimitTier(long defaultCapacity, int defaultDurationMinutes) {
        this.defaultCapacity = defaultCapacity;
        this.defaultDurationMinutes = defaultDurationMinutes;
    }

    public long getDefaultCapacity() {
        return defaultCapacity;
    }

    public int getDefaultDurationMinutes() {
        return defaultDurationMinutes;
    }
}
