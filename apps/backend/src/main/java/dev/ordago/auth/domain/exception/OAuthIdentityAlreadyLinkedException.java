package dev.ordago.auth.domain.exception;

import dev.ordago.shared.domain.exception.DomainException;

public class OAuthIdentityAlreadyLinkedException extends DomainException {

    public OAuthIdentityAlreadyLinkedException() {
        super("OAUTH_IDENTITY_ALREADY_LINKED", "auth.oauth.identity_already_linked", 409);
    }
}
