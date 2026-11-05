package dev.ordago.auth.domain.model;

import java.util.Objects;

public record OAuthUserInfo(
        OAuthProvider provider,
        String providerSubject,
        String email,
        boolean emailVerified,
        String displayName,
        String avatarUrl
) {
    public OAuthUserInfo {
        Objects.requireNonNull(provider, "OAuthProvider cannot be null");
        Objects.requireNonNull(providerSubject, "providerSubject cannot be null");
        if (providerSubject.isBlank()) {
            throw new IllegalArgumentException("providerSubject cannot be blank");
        }
    }
}
