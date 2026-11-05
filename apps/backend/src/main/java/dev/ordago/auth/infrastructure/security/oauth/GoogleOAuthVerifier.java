package dev.ordago.auth.infrastructure.security.oauth;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import dev.ordago.auth.domain.exception.InvalidOAuthTokenException;
import dev.ordago.auth.domain.model.OAuthProvider;
import dev.ordago.auth.domain.model.OAuthUserInfo;
import dev.ordago.auth.domain.port.out.OAuthVerifierStrategy;

@ApplicationScoped
public class GoogleOAuthVerifier implements OAuthVerifierStrategy {

    private static final Logger LOG = Logger.getLogger(GoogleOAuthVerifier.class);
    private static final String DEFAULT_TOKEN_INFO_URL = "https://oauth2.googleapis.com/tokeninfo?id_token=";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String googleClientId;
    private final String tokenInfoUrl;

    @Inject
    public GoogleOAuthVerifier(
            ObjectMapper objectMapper,
            @ConfigProperty(name = "ordago.auth.oauth.google.client-id", defaultValue = "") String googleClientId
    ) {
        this(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build(),
                objectMapper,
                googleClientId != null ? googleClientId.trim() : "",
                DEFAULT_TOKEN_INFO_URL
        );
    }

    GoogleOAuthVerifier(HttpClient httpClient, ObjectMapper objectMapper, String googleClientId) {
        this(httpClient, objectMapper, googleClientId, DEFAULT_TOKEN_INFO_URL);
    }

    GoogleOAuthVerifier(HttpClient httpClient, ObjectMapper objectMapper, String googleClientId, String tokenInfoUrl) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient cannot be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper cannot be null");
        this.googleClientId = googleClientId != null ? googleClientId.trim() : "";
        this.tokenInfoUrl = tokenInfoUrl != null ? tokenInfoUrl : DEFAULT_TOKEN_INFO_URL;
    }

    @Override
    public boolean supports(OAuthProvider provider) {
        return provider == OAuthProvider.GOOGLE;
    }

    @Override
    public OAuthUserInfo verify(String token) {
        if (token == null || token.isBlank()) {
            throw new InvalidOAuthTokenException("OAuth token cannot be null or blank");
        }

        String encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(tokenInfoUrl + encodedToken))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new InvalidOAuthTokenException("Invalid Google token");
            }

            JsonNode root = objectMapper.readTree(response.body());

            String sub = root.path("sub").asText(null);
            if (sub == null || sub.isBlank()) {
                throw new InvalidOAuthTokenException("Google token missing subject");
            }

            String aud = root.path("aud").asText(null);
            if (!googleClientId.isEmpty() && !googleClientId.equals(aud)) {
                throw new InvalidOAuthTokenException("Google token audience mismatch");
            }

            String email = root.path("email").asText(null);
            boolean emailVerified = root.path("email_verified").asBoolean(false)
                    || "true".equalsIgnoreCase(root.path("email_verified").asText(null));

            if (email == null || email.isBlank() || !emailVerified) {
                throw new InvalidOAuthTokenException("auth.oauth.invalid_token");
            }

            String displayName = root.hasNonNull("name") ? root.path("name").asText() : null;
            String avatarUrl = root.hasNonNull("picture") ? root.path("picture").asText() : null;

            return new OAuthUserInfo(
                    OAuthProvider.GOOGLE,
                    sub,
                    email,
                    emailVerified,
                    displayName,
                    avatarUrl
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.warn("Google OAuth token verification interrupted", e);
            throw new InvalidOAuthTokenException();
        } catch (IOException e) {
            LOG.warn("Google OAuth token verification failed", e);
            throw new InvalidOAuthTokenException();
        }
    }
}
