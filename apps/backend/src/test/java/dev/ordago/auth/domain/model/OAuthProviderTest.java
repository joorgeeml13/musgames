package dev.ordago.auth.domain.model;

import org.junit.jupiter.api.Test;
import dev.ordago.auth.domain.exception.OAuthProviderNotSupportedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OAuthProviderTest {

    @Test
    void shouldParseGoogleCaseInsensitive() {
        assertThat(OAuthProvider.fromString("google")).isEqualTo(OAuthProvider.GOOGLE);
        assertThat(OAuthProvider.fromString("GOOGLE")).isEqualTo(OAuthProvider.GOOGLE);
        assertThat(OAuthProvider.fromString("  Google  ")).isEqualTo(OAuthProvider.GOOGLE);
    }

    @Test
    void shouldParseDiscordCaseInsensitive() {
        assertThat(OAuthProvider.fromString("discord")).isEqualTo(OAuthProvider.DISCORD);
        assertThat(OAuthProvider.fromString("DISCORD")).isEqualTo(OAuthProvider.DISCORD);
        assertThat(OAuthProvider.fromString("  Discord  ")).isEqualTo(OAuthProvider.DISCORD);
    }

    @Test
    void shouldRejectUnsupportedOrBlankProvider() {
        assertThatThrownBy(() -> OAuthProvider.fromString("facebook"))
                .isInstanceOf(OAuthProviderNotSupportedException.class);

        assertThatThrownBy(() -> OAuthProvider.fromString(""))
                .isInstanceOf(OAuthProviderNotSupportedException.class);

        assertThatThrownBy(() -> OAuthProvider.fromString(null))
                .isInstanceOf(OAuthProviderNotSupportedException.class);
    }
}
