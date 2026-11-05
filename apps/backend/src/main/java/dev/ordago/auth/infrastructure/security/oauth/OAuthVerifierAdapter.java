package dev.ordago.auth.infrastructure.security.oauth;

import java.util.Objects;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import dev.ordago.auth.domain.exception.OAuthProviderNotSupportedException;
import dev.ordago.auth.domain.model.OAuthProvider;
import dev.ordago.auth.domain.model.OAuthUserInfo;
import dev.ordago.auth.domain.port.out.OAuthVerifierPort;
import dev.ordago.auth.domain.port.out.OAuthVerifierStrategy;

@ApplicationScoped
public class OAuthVerifierAdapter implements OAuthVerifierPort {

    private final Instance<OAuthVerifierStrategy> strategies;

    @Inject
    public OAuthVerifierAdapter(Instance<OAuthVerifierStrategy> strategies) {
        this.strategies = Objects.requireNonNull(strategies, "strategies cannot be null");
    }

    @Override
    public OAuthUserInfo verify(OAuthProvider provider, String token) {
        if (provider == null) {
            throw new OAuthProviderNotSupportedException(null);
        }
        for (OAuthVerifierStrategy strategy : strategies) {
            if (strategy.supports(provider)) {
                return strategy.verify(token);
            }
        }
        throw new OAuthProviderNotSupportedException(provider.name());
    }
}
