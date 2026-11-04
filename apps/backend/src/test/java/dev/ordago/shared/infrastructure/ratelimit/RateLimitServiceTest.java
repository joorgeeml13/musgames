package dev.ordago.shared.infrastructure.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import io.github.bucket4j.ConsumptionProbe;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RateLimitService Unit Tests")
class RateLimitServiceTest {

    private RateLimitService service;

    @BeforeEach
    void setUp() {
        service = RateLimitService.createWithDefaults(true);
    }

    @Nested
    @DisplayName("1. Token Consumption and Capacity Enforcement")
    class TokenConsumptionTests {

        @Test
        @DisplayName("Allows requests up to capacity and rejects subsequent ones")
        void allowsUpToCapacityAndRejectsSubsequent() {
            String clientIp = "192.0.2.10";
            long capacity = service.getCapacity(RateLimitTier.LOGIN); // 5

            for (int i = 0; i < capacity; i++) {
                ConsumptionProbe probe = service.consumeToken(clientIp, RateLimitTier.LOGIN);
                assertThat(probe.isConsumed())
                        .as("Request %d within capacity should be consumed", i + 1)
                        .isTrue();
                assertThat(probe.getRemainingTokens())
                        .as("Remaining tokens after request %d", i + 1)
                        .isEqualTo(capacity - 1 - i);
            }

            ConsumptionProbe rejectedProbe = service.consumeToken(clientIp, RateLimitTier.LOGIN);
            assertThat(rejectedProbe.isConsumed())
                    .as("Request exceeding capacity must be rejected")
                    .isFalse();
            assertThat(rejectedProbe.getRemainingTokens())
                    .as("Remaining tokens on rejection should be 0")
                    .isZero();
            assertThat(rejectedProbe.getNanosToWaitForRefill())
                    .as("Nanos to wait for refill should be greater than zero")
                    .isPositive();
        }
    }

    @Nested
    @DisplayName("2. Client IP Isolation")
    class ClientIpIsolationTests {

        @Test
        @DisplayName("Independent buckets per client IP (IP A hitting limit does not affect IP B)")
        void independentBucketsPerClientIp() {
            String ipA = "198.51.100.1";
            String ipB = "198.51.100.2";
            long capacity = service.getCapacity(RateLimitTier.REGISTER); // 3

            for (int i = 0; i < capacity; i++) {
                ConsumptionProbe probe = service.consumeToken(ipA, RateLimitTier.REGISTER);
                assertThat(probe.isConsumed()).isTrue();
            }

            ConsumptionProbe probeA = service.consumeToken(ipA, RateLimitTier.REGISTER);
            assertThat(probeA.isConsumed())
                    .as("IP A must be exhausted and rejected")
                    .isFalse();

            ConsumptionProbe probeB = service.consumeToken(ipB, RateLimitTier.REGISTER);
            assertThat(probeB.isConsumed())
                    .as("IP B must not be affected by IP A's rate limit")
                    .isTrue();
            assertThat(probeB.getRemainingTokens()).isEqualTo(capacity - 1);
        }
    }

    @Nested
    @DisplayName("3. Tier Isolation")
    class TierIsolationTests {

        @Test
        @DisplayName("Independent buckets per tier (Login limit does not block Register)")
        void independentBucketsPerTier() {
            String clientIp = "203.0.113.50";
            long loginCapacity = service.getCapacity(RateLimitTier.LOGIN);

            for (int i = 0; i < loginCapacity; i++) {
                ConsumptionProbe probe = service.consumeToken(clientIp, RateLimitTier.LOGIN);
                assertThat(probe.isConsumed()).isTrue();
            }

            ConsumptionProbe loginProbe = service.consumeToken(clientIp, RateLimitTier.LOGIN);
            assertThat(loginProbe.isConsumed())
                    .as("LOGIN must be exhausted")
                    .isFalse();

            ConsumptionProbe registerProbe = service.consumeToken(clientIp, RateLimitTier.REGISTER);
            assertThat(registerProbe.isConsumed())
                    .as("REGISTER tier should be independent and allow consumption")
                    .isTrue();
            assertThat(registerProbe.getRemainingTokens())
                    .isEqualTo(service.getCapacity(RateLimitTier.REGISTER) - 1);

            ConsumptionProbe refreshProbe = service.consumeToken(clientIp, RateLimitTier.REFRESH);
            assertThat(refreshProbe.isConsumed())
                    .as("REFRESH tier should be independent and allow consumption")
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("4. Rate Limiting Toggle")
    class DisabledToggleTests {

        @Test
        @DisplayName("Toggle when disabled allows unlimited requests")
        void disabledToggleAllowsUnlimitedRequests() {
            service.setEnabled(false);
            String clientIp = "192.168.1.100";
            long capacity = service.getCapacity(RateLimitTier.LOGIN);

            // Consume many more requests than capacity
            for (int i = 0; i < capacity * 3; i++) {
                ConsumptionProbe probe = service.consumeToken(clientIp, RateLimitTier.LOGIN);
                assertThat(probe.isConsumed())
                        .as("Every request must succeed when rate limiting is disabled")
                        .isTrue();
            }
        }
    }

    @Nested
    @DisplayName("5. Cache Management and Eviction")
    class CacheManagementTests {

        @Test
        @DisplayName("Cache clears successfully on clearCache()")
        void clearCacheResetsBuckets() {
            String clientIp = "10.0.0.1";
            long capacity = service.getCapacity(RateLimitTier.PASSWORD_RESET);

            for (int i = 0; i < capacity; i++) {
                service.consumeToken(clientIp, RateLimitTier.PASSWORD_RESET);
            }

            assertThat(service.consumeToken(clientIp, RateLimitTier.PASSWORD_RESET).isConsumed())
                    .isFalse();

            service.clearCache();
            assertThat(service.cacheSize()).isZero();

            ConsumptionProbe probeAfterClear = service.consumeToken(clientIp, RateLimitTier.PASSWORD_RESET);
            assertThat(probeAfterClear.isConsumed())
                    .as("New token bucket should be created after cache invalidation")
                    .isTrue();
            assertThat(probeAfterClear.getRemainingTokens()).isEqualTo(capacity - 1);
        }
    }
}
