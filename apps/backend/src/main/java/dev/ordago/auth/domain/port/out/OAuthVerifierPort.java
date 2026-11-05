package dev.ordago.auth.domain.port.out;

import dev.ordago.auth.domain.model.OAuthProvider;
import dev.ordago.auth.domain.model.OAuthUserInfo;

public interface OAuthVerifierPort {

    OAuthUserInfo verify(OAuthProvider provider, String token);
}
