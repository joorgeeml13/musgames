package dev.ordago.auth.domain.model;

import java.util.Locale;
import dev.ordago.auth.domain.exception.OAuthProviderNotSupportedException;

public enum OAuthProvider {
    GOOGLE,
    DISCORD;

    public static OAuthProvider fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new OAuthProviderNotSupportedException(value);
        }
        try {
            return OAuthProvider.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new OAuthProviderNotSupportedException(value);
        }
    }
}
