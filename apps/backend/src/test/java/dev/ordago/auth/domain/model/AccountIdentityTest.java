package dev.ordago.auth.domain.model;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountIdentityTest {

    @Test
    void shouldCreateValidAccountIdentity() {
        UUID id = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        Instant now = Instant.now();

        AccountIdentity identity = new AccountIdentity(
                id,
                accountId,
                OAuthProvider.GOOGLE,
                "sub-12345",
                "user@example.com",
                now,
                now
        );

        assertThat(identity.id()).isEqualTo(id);
        assertThat(identity.accountId()).isEqualTo(accountId);
        assertThat(identity.provider()).isEqualTo(OAuthProvider.GOOGLE);
        assertThat(identity.providerSubject()).isEqualTo("sub-12345");
        assertThat(identity.email()).isEqualTo("user@example.com");
    }

    @Test
    void shouldRejectNullOrBlankFields() {
        UUID id = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        Instant now = Instant.now();

        assertThatThrownBy(() -> new AccountIdentity(null, accountId, OAuthProvider.GOOGLE, "sub", "email", now, now))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> new AccountIdentity(id, null, OAuthProvider.GOOGLE, "sub", "email", now, now))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> new AccountIdentity(id, accountId, null, "sub", "email", now, now))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> new AccountIdentity(id, accountId, OAuthProvider.GOOGLE, null, "email", now, now))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> new AccountIdentity(id, accountId, OAuthProvider.GOOGLE, "  ", "email", now, now))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
