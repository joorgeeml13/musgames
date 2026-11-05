package dev.ordago.auth.domain.port.out;

import dev.ordago.auth.domain.model.OAuthProvider;
import dev.ordago.auth.domain.model.OAuthUserInfo;

public interface OAuthVerifierStrategy {

    boolean supports(OAuthProvider provider);

    OAuthUserInfo verify(String token);
}
