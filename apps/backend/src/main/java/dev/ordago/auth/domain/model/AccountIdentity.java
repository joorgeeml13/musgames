package dev.ordago.auth.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record AccountIdentity(
        UUID id,
        UUID accountId,
        OAuthProvider provider,
        String providerSubject,
        String email,
        Instant createdAt,
        Instant updatedAt
) {
    public AccountIdentity {
        Objects.requireNonNull(id, "AccountIdentity id cannot be null");
        Objects.requireNonNull(accountId, "AccountIdentity accountId cannot be null");
        Objects.requireNonNull(provider, "AccountIdentity provider cannot be null");
        Objects.requireNonNull(providerSubject, "AccountIdentity providerSubject cannot be null");
        if (providerSubject.isBlank()) {
            throw new IllegalArgumentException("AccountIdentity providerSubject cannot be blank");
        }
        Objects.requireNonNull(createdAt, "AccountIdentity createdAt cannot be null");
        Objects.requireNonNull(updatedAt, "AccountIdentity updatedAt cannot be null");
    }

    public static AccountIdentity createNew(
            UUID id,
            UUID accountId,
            OAuthProvider provider,
            String providerSubject,
            String email,
            Instant now
    ) {
        Instant timestamp = now != null ? now : Instant.now();
        return new AccountIdentity(id, accountId, provider, providerSubject, email, timestamp, timestamp);
    }
}
