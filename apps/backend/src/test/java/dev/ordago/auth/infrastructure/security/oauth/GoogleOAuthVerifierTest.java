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

class GoogleOAuthVerifierTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private HttpServer server;
    private int port;
    private String responseBody;
    private int responseStatus;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/tokeninfo", exchange -> {
            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(responseStatus, bytes.length);
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
    void shouldVerifyGoogleTokenSuccessfully() {
        responseStatus = 200;
        responseBody = """
                {
                    "sub": "google-12345",
                    "aud": "my-client-id",
                    "email": "user@gmail.com",
                    "email_verified": "true",
                    "name": "Google User",
                    "picture": "https://lh3.googleusercontent.com/avatar"
                }
                """;

        String url = "http://localhost:" + port + "/tokeninfo?id_token=";
        GoogleOAuthVerifier verifier = new GoogleOAuthVerifier(
                HttpClient.newHttpClient(),
                objectMapper,
                "my-client-id",
                url
        );

        assertThat(verifier.supports(OAuthProvider.GOOGLE)).isTrue();
        assertThat(verifier.supports(OAuthProvider.DISCORD)).isFalse();

        OAuthUserInfo info = verifier.verify("valid-token");
        assertThat(info.provider()).isEqualTo(OAuthProvider.GOOGLE);
        assertThat(info.providerSubject()).isEqualTo("google-12345");
        assertThat(info.email()).isEqualTo("user@gmail.com");
        assertThat(info.emailVerified()).isTrue();
        assertThat(info.displayName()).isEqualTo("Google User");
        assertThat(info.avatarUrl()).isEqualTo("https://lh3.googleusercontent.com/avatar");
    }

    @Test
    void shouldFailWhenGoogleReturnsNon200() {
        responseStatus = 400;
        responseBody = "{\"error\": \"invalid_token\"}";

        String url = "http://localhost:" + port + "/tokeninfo?id_token=";
        GoogleOAuthVerifier verifier = new GoogleOAuthVerifier(
                HttpClient.newHttpClient(),
                objectMapper,
                "",
                url
        );

        assertThatThrownBy(() -> verifier.verify("bad-token"))
                .isInstanceOf(InvalidOAuthTokenException.class);
    }

    @Test
    void shouldFailWhenAudienceMismatches() {
        responseStatus = 200;
        responseBody = """
                {
                    "sub": "google-12345",
                    "aud": "wrong-client-id",
                    "email": "user@gmail.com",
                    "email_verified": "true"
                }
                """;

        String url = "http://localhost:" + port + "/tokeninfo?id_token=";
        GoogleOAuthVerifier verifier = new GoogleOAuthVerifier(
                HttpClient.newHttpClient(),
                objectMapper,
                "expected-client-id",
                url
        );

        assertThatThrownBy(() -> verifier.verify("token"))
                .isInstanceOf(InvalidOAuthTokenException.class)
                .hasMessageContaining("audience mismatch");
    }

    @Test
    void shouldFailWhenGoogleEmailNotVerified() {
        responseStatus = 200;
        responseBody = """
                {
                    "sub": "google-12345",
                    "aud": "my-client-id",
                    "email": "user@gmail.com",
                    "email_verified": "false"
                }
                """;

        String url = "http://localhost:" + port + "/tokeninfo?id_token=";
        GoogleOAuthVerifier verifier = new GoogleOAuthVerifier(
                HttpClient.newHttpClient(),
                objectMapper,
                "my-client-id",
                url
        );

        assertThatThrownBy(() -> verifier.verify("token"))
                .isInstanceOf(InvalidOAuthTokenException.class)
                .hasMessageContaining("auth.oauth.invalid_token");
    }

    @Test
    void shouldFailWhenGoogleEmailIsMissing() {
        responseStatus = 200;
        responseBody = """
                {
                    "sub": "google-12345",
                    "aud": "my-client-id",
                    "email_verified": "true"
                }
                """;

        String url = "http://localhost:" + port + "/tokeninfo?id_token=";
        GoogleOAuthVerifier verifier = new GoogleOAuthVerifier(
                HttpClient.newHttpClient(),
                objectMapper,
                "my-client-id",
                url
        );

        assertThatThrownBy(() -> verifier.verify("token"))
                .isInstanceOf(InvalidOAuthTokenException.class)
                .hasMessageContaining("auth.oauth.invalid_token");
    }

    @Test
    void shouldMaskNetworkErrorsWithoutLeakingInternalDetails() {
        server.stop(0);
        server = null;

        String url = "http://localhost:" + port + "/tokeninfo?id_token=";
        GoogleOAuthVerifier verifier = new GoogleOAuthVerifier(
                HttpClient.newHttpClient(),
                objectMapper,
                "",
                url
        );

        assertThatThrownBy(() -> verifier.verify("token"))
                .isInstanceOf(InvalidOAuthTokenException.class)
                .hasMessage("auth.oauth.invalid_token");
    }
}
