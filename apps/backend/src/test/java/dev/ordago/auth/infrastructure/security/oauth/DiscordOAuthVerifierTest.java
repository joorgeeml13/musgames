package dev.ordago.auth.infrastructure.security.oauth;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import dev.ordago.auth.domain.exception.InvalidOAuthTokenException;
import dev.ordago.auth.domain.model.OAuthProvider;
import dev.ordago.auth.domain.model.OAuthUserInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DiscordOAuthVerifierTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private HttpServer server;
    private int port;
    private String responseBody;
    private int responseStatus;
    private String capturedAuthHeader;
    private String tokenInfoResponseBody;
    private int tokenInfoResponseStatus;
    private String capturedTokenInfoAuthHeader;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/users/@me", exchange -> {
            capturedAuthHeader = exchange.getRequestHeaders().getFirst("Authorization");
            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(responseStatus, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        server.createContext("/oauth2/@me", exchange -> {
            capturedTokenInfoAuthHeader = exchange.getRequestHeaders().getFirst("Authorization");
            byte[] bytes = (tokenInfoResponseBody != null ? tokenInfoResponseBody : "").getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(tokenInfoResponseStatus != 0 ? tokenInfoResponseStatus : 200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        server.start();
        port = server.getAddress().getPort();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void shouldVerifyDiscordTokenSuccessfully() {
        responseStatus = 200;
        responseBody = """
                {
                    "id": "discord-98765",
                    "username": "musmaster",
                    "global_name": "Mus Master",
                    "email": "musmaster@discord.com",
                    "verified": true,
                    "avatar": "a_1234567890abcdef"
                }
                """;

        String url = "http://localhost:" + port + "/users/@me";
        DiscordOAuthVerifier verifier = new DiscordOAuthVerifier(
                HttpClient.newHttpClient(),
                objectMapper,
                url
        );

        assertThat(verifier.supports(OAuthProvider.DISCORD)).isTrue();
        assertThat(verifier.supports(OAuthProvider.GOOGLE)).isFalse();

        OAuthUserInfo info = verifier.verify("valid-discord-token");
        assertThat(info.provider()).isEqualTo(OAuthProvider.DISCORD);
        assertThat(info.providerSubject()).isEqualTo("discord-98765");
        assertThat(info.email()).isEqualTo("musmaster@discord.com");
        assertThat(info.emailVerified()).isTrue();
        assertThat(info.displayName()).isEqualTo("Mus Master");
        assertThat(info.avatarUrl()).isEqualTo("https://cdn.discordapp.com/avatars/discord-98765/a_1234567890abcdef.png");
        assertThat(capturedAuthHeader).isEqualTo("Bearer valid-discord-token");
    }

    @Test
    void shouldRejectWhenEmailNotVerifiedInDiscord() {
        responseStatus = 200;
        responseBody = """
                {
                    "id": "discord-98765",
                    "username": "unverified",
                    "email": "unverified@discord.com",
                    "verified": false
                }
                """;

        String url = "http://localhost:" + port + "/users/@me";
        DiscordOAuthVerifier verifier = new DiscordOAuthVerifier(
                HttpClient.newHttpClient(),
                objectMapper,
                url
        );

        assertThatThrownBy(() -> verifier.verify("unverified-token"))
                .isInstanceOf(InvalidOAuthTokenException.class)
                .hasMessageContaining("Discord email is not verified");
    }

    @Test
    void shouldRejectWhenDiscordReturnsNon200() {
        responseStatus = 401;
        responseBody = "{\"message\": \"401: Unauthorized\"}";

        String url = "http://localhost:" + port + "/users/@me";
        DiscordOAuthVerifier verifier = new DiscordOAuthVerifier(
                HttpClient.newHttpClient(),
                objectMapper,
                url
        );

        assertThatThrownBy(() -> verifier.verify("bad-token"))
                .isInstanceOf(InvalidOAuthTokenException.class);
    }

    @Test
    void shouldVerifyDiscordTokenWithMatchingApplicationId() {
        tokenInfoResponseStatus = 200;
        tokenInfoResponseBody = """
                {
                    "application": {
                        "id": "expected-app-id",
                        "name": "Mus Game"
                    }
                }
                """;
        responseStatus = 200;
        responseBody = """
                {
                    "id": "discord-123",
                    "username": "verifieduser",
                    "email": "verified@discord.com",
                    "verified": true
                }
                """;

        String userUrl = "http://localhost:" + port + "/users/@me";
        String tokenUrl = "http://localhost:" + port + "/oauth2/@me";
        DiscordOAuthVerifier verifier = new DiscordOAuthVerifier(
                HttpClient.newHttpClient(),
                objectMapper,
                "expected-app-id",
                userUrl,
                tokenUrl
        );

        OAuthUserInfo info = verifier.verify("good-token");
        assertThat(info.providerSubject()).isEqualTo("discord-123");
        assertThat(capturedTokenInfoAuthHeader).isEqualTo("Bearer good-token");
        assertThat(capturedAuthHeader).isEqualTo("Bearer good-token");
    }

    @Test
    void shouldRejectWhenDiscordApplicationIdMismatches() {
        tokenInfoResponseStatus = 200;
        tokenInfoResponseBody = """
                {
                    "application": {
                        "id": "wrong-app-id",
                        "name": "Other Game"
                    }
                }
                """;

        String userUrl = "http://localhost:" + port + "/users/@me";
        String tokenUrl = "http://localhost:" + port + "/oauth2/@me";
        DiscordOAuthVerifier verifier = new DiscordOAuthVerifier(
                HttpClient.newHttpClient(),
                objectMapper,
                "expected-app-id",
                userUrl,
                tokenUrl
        );

        assertThatThrownBy(() -> verifier.verify("foreign-token"))
                .isInstanceOf(InvalidOAuthTokenException.class)
                .hasMessageContaining("application ID mismatch");
    }

    @Test
    void shouldRejectWhenDiscordOAuth2MeReturnsNon200() {
        tokenInfoResponseStatus = 401;
        tokenInfoResponseBody = "{\"message\": \"Invalid token\"}";

        String userUrl = "http://localhost:" + port + "/users/@me";
        String tokenUrl = "http://localhost:" + port + "/oauth2/@me";
        DiscordOAuthVerifier verifier = new DiscordOAuthVerifier(
                HttpClient.newHttpClient(),
                objectMapper,
                "expected-app-id",
                userUrl,
                tokenUrl
        );

        assertThatThrownBy(() -> verifier.verify("bad-token"))
                .isInstanceOf(InvalidOAuthTokenException.class);
    }

    @Test
    void shouldMaskNetworkErrorsWithoutLeakingInternalDetails() {
        server.stop(0);
        server = null;

        String userUrl = "http://localhost:" + port + "/users/@me";
        DiscordOAuthVerifier verifier = new DiscordOAuthVerifier(
                HttpClient.newHttpClient(),
                objectMapper,
                userUrl
        );

        assertThatThrownBy(() -> verifier.verify("token"))
                .isInstanceOf(InvalidOAuthTokenException.class)
                .hasMessage("auth.oauth.invalid_token");
    }
}
