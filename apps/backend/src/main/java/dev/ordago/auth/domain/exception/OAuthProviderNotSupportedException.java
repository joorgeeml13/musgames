package dev.ordago.auth.domain.exception;

import dev.ordago.shared.domain.exception.DomainException;

public class OAuthProviderNotSupportedException extends DomainException {

    public OAuthProviderNotSupportedException(String provider) {
        super("OAUTH_PROVIDER_NOT_SUPPORTED", "auth.oauth.provider_not_supported", 400, provider != null ? provider : "unknown");
    }
}
