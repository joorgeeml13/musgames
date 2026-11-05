package dev.ordago.auth.infrastructure.security.oauth;

import java.lang.reflect.Proxy;
import java.util.List;
import jakarta.enterprise.inject.Instance;
import org.junit.jupiter.api.Test;
import dev.ordago.auth.domain.exception.OAuthProviderNotSupportedException;
import dev.ordago.auth.domain.model.OAuthProvider;
import dev.ordago.auth.domain.model.OAuthUserInfo;
import dev.ordago.auth.domain.port.out.OAuthVerifierStrategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OAuthVerifierAdapterTest {

    @SuppressWarnings("unchecked")
    private Instance<OAuthVerifierStrategy> createInstance(List<OAuthVerifierStrategy> strategies) {
        return (Instance<OAuthVerifierStrategy>) Proxy.newProxyInstance(
                Instance.class.getClassLoader(),
                new Class<?>[]{Instance.class},
                (proxy, method, args) -> {
                    if ("iterator".equals(method.getName())) {
                        return strategies.iterator();
                    }
                    throw new UnsupportedOperationException();
                }
        );
    }

    @Test
    void shouldDelegateToSupportedStrategy() {
        OAuthVerifierStrategy googleStrategy = new OAuthVerifierStrategy() {
            @Override
            public boolean supports(OAuthProvider provider) {
                return provider == OAuthProvider.GOOGLE;
            }

            @Override
            public OAuthUserInfo verify(String token) {
                return new OAuthUserInfo(OAuthProvider.GOOGLE, "sub-g", "g@ordago.dev", true, "G", null);
            }
        };

        OAuthVerifierStrategy discordStrategy = new OAuthVerifierStrategy() {
            @Override
            public boolean supports(OAuthProvider provider) {
                return provider == OAuthProvider.DISCORD;
            }

            @Override
            public OAuthUserInfo verify(String token) {
                return new OAuthUserInfo(OAuthProvider.DISCORD, "sub-d", "d@ordago.dev", true, "D", null);
            }
        };

        OAuthVerifierAdapter adapter = new OAuthVerifierAdapter(createInstance(List.of(googleStrategy, discordStrategy)));

        OAuthUserInfo result = adapter.verify(OAuthProvider.GOOGLE, "token-g");
        assertThat(result.provider()).isEqualTo(OAuthProvider.GOOGLE);
        assertThat(result.providerSubject()).isEqualTo("sub-g");

        OAuthUserInfo discordResult = adapter.verify(OAuthProvider.DISCORD, "token-d");
        assertThat(discordResult.provider()).isEqualTo(OAuthProvider.DISCORD);
        assertThat(discordResult.providerSubject()).isEqualTo("sub-d");
    }

    @Test
    void shouldThrowWhenNoStrategySupportsProvider() {
        OAuthVerifierAdapter adapter = new OAuthVerifierAdapter(createInstance(List.of()));

        assertThatThrownBy(() -> adapter.verify(OAuthProvider.GOOGLE, "token"))
                .isInstanceOf(OAuthProviderNotSupportedException.class);
    }

    @Test
    void shouldThrowWhenProviderIsNull() {
        OAuthVerifierAdapter adapter = new OAuthVerifierAdapter(createInstance(List.of()));

        assertThatThrownBy(() -> adapter.verify(null, "token"))
                .isInstanceOf(OAuthProviderNotSupportedException.class);
    }
}
