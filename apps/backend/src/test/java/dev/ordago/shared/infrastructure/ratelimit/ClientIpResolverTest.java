package dev.ordago.shared.infrastructure.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ClientIpResolver Unit Tests")
class ClientIpResolverTest {

    private ClientIpResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new ClientIpResolver();
    }

    @Test
    @DisplayName("Takes first IP from comma-separated X-Forwarded-For")
    void takesFirstIpFromXForwardedFor() {
        String forwarded = "203.0.113.195, 70.41.3.18, 150.172.238.178";
        String ip = resolver.resolve(forwarded, "70.41.3.18", "10.0.0.1");
        assertThat(ip).isEqualTo("203.0.113.195");
    }

    @Test
    @DisplayName("Uses X-Real-IP when X-Forwarded-For is missing")
    void usesRealIpWhenForwardedMissing() {
        String ip = resolver.resolve(null, "198.51.100.42", "10.0.0.1");
        assertThat(ip).isEqualTo("198.51.100.42");
    }

    @Test
    @DisplayName("Uses remote address when proxy headers are missing")
    void usesRemoteAddressWhenProxyHeadersMissing() {
        String ip = resolver.resolve(null, null, "192.168.1.55");
        assertThat(ip).isEqualTo("192.168.1.55");
    }

    @Test
    @DisplayName("Falls back to 127.0.0.1 when all sources are missing or blank")
    void fallsBackToLocalhostWhenAllMissing() {
        assertThat(resolver.resolve(null, null, null)).isEqualTo("127.0.0.1");
        assertThat(resolver.resolve("   ", "", "  ")).isEqualTo("127.0.0.1");
    }

    @Test
    @DisplayName("Strips port from IPv4 address")
    void stripsPortFromIpv4() {
        String ip = resolver.resolve("198.51.100.1:8080", null, null);
        assertThat(ip).isEqualTo("198.51.100.1");
    }

    @Test
    @DisplayName("Handles bracketed IPv6 with port")
    void handlesBracketedIpv6WithPort() {
        String ip = resolver.resolve("[2001:db8:85a3::8a2e:370:7334]:443", null, null);
        assertThat(ip).isEqualTo("2001:db8:85a3::8a2e:370:7334");
    }

    @Test
    @DisplayName("Handles standard unbracketed IPv6 without port")
    void handlesStandardIpv6() {
        String ipv6 = "2001:db8:85a3:0000:0000:8a2e:0370:7334";
        String ip = resolver.resolve(ipv6, null, null);
        assertThat(ip).isEqualTo(ipv6);
    }

    @Test
    @DisplayName("Protects against length overflow (>45 chars)")
    void protectsAgainstLengthOverflow() {
        String overlyLongIp = "2001:0db8:85a3:0000:0000:8a2e:0370:7334:extra:overflow:content:beyond:valid";
        String ip = resolver.resolve(overlyLongIp, null, null);
        assertThat(ip).hasSize(45);
        assertThat(ip).isEqualTo(overlyLongIp.substring(0, 45));
    }
}
