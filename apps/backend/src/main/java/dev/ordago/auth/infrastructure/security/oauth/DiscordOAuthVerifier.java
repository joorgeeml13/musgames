package dev.ordago.auth.infrastructure.security.oauth;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
public class DiscordOAuthVerifier implements OAuthVerifierStrategy {

    private static final Logger LOG = Logger.getLogger(DiscordOAuthVerifier.class);
    private static final String DEFAULT_USER_INFO_URL = "https://discord.com/api/v10/users/@me";
    private static final String DEFAULT_TOKEN_INFO_URL = "https://discord.com/api/v10/oauth2/@me";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String discordClientId;
    private final String userInfoUrl;
    private final String tokenInfoUrl;

    @Inject
    public DiscordOAuthVerifier(
            ObjectMapper objectMapper,
            @ConfigProperty(name = "ordago.auth.oauth.discord.client-id", defaultValue = "") String discordClientId
    ) {
        this(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build(),
                objectMapper,
                discordClientId != null ? discordClientId.trim() : "",
                DEFAULT_USER_INFO_URL,
                DEFAULT_TOKEN_INFO_URL
        );
    }

    DiscordOAuthVerifier(HttpClient httpClient, ObjectMapper objectMapper) {
        this(httpClient, objectMapper, "", DEFAULT_USER_INFO_URL, DEFAULT_TOKEN_INFO_URL);
    }

    DiscordOAuthVerifier(HttpClient httpClient, ObjectMapper objectMapper, String userInfoUrl) {
        this(httpClient, objectMapper, "", userInfoUrl, DEFAULT_TOKEN_INFO_URL);
    }

    DiscordOAuthVerifier(HttpClient httpClient, ObjectMapper objectMapper, String discordClientId, String userInfoUrl, String tokenInfoUrl) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient cannot be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper cannot be null");
        this.discordClientId = discordClientId != null ? discordClientId.trim() : "";
        this.userInfoUrl = userInfoUrl != null ? userInfoUrl : DEFAULT_USER_INFO_URL;
        this.tokenInfoUrl = tokenInfoUrl != null ? tokenInfoUrl : DEFAULT_TOKEN_INFO_URL;
    }

    @Override
    public boolean supports(OAuthProvider provider) {
        return provider == OAuthProvider.DISCORD;
    }

    @Override
    public OAuthUserInfo verify(String token) {
        if (token == null || token.isBlank()) {
            throw new InvalidOAuthTokenException("OAuth token cannot be null or blank");
        }

        try {
            if (!discordClientId.isEmpty()) {
                HttpRequest tokenInfoRequest = HttpRequest.newBuilder()
                        .uri(URI.create(tokenInfoUrl))
                        .header("Authorization", "Bearer " + token)
                        .timeout(Duration.ofSeconds(5))
                        .GET()
                        .build();

                HttpResponse<String> tokenInfoResponse = httpClient.send(tokenInfoRequest, HttpResponse.BodyHandlers.ofString());
                if (tokenInfoResponse.statusCode() != 200) {
                    throw new InvalidOAuthTokenException("Invalid Discord token");
                }

                JsonNode tokenInfoRoot = objectMapper.readTree(tokenInfoResponse.body());
                String appId = tokenInfoRoot.path("application").path("id").asText(null);
                if (!discordClientId.equals(appId)) {
                    throw new InvalidOAuthTokenException("Discord token application ID mismatch");
                }
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(userInfoUrl))
                    .header("Authorization", "Bearer " + token)
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new InvalidOAuthTokenException("Invalid Discord token");
            }

            JsonNode root = objectMapper.readTree(response.body());

            String id = root.path("id").asText(null);
            if (id == null || id.isBlank()) {
                throw new InvalidOAuthTokenException("Discord token missing user id");
            }

            String email = root.path("email").asText(null);
            boolean verified = root.path("verified").asBoolean(false);

            if (email == null || email.isBlank() || !verified) {
                throw new InvalidOAuthTokenException("Discord email is not verified");
            }

            String username = root.hasNonNull("username") ? root.path("username").asText() : null;
            String globalName = root.hasNonNull("global_name") ? root.path("global_name").asText() : null;
            String displayName = (globalName != null && !globalName.isBlank()) ? globalName : username;

            String avatar = root.hasNonNull("avatar") ? root.path("avatar").asText() : null;
            String avatarUrl = (avatar != null && !avatar.isBlank())
                    ? "https://cdn.discordapp.com/avatars/" + id + "/" + avatar + ".png"
                    : null;

            return new OAuthUserInfo(
                    OAuthProvider.DISCORD,
                    id,
                    email,
                    verified,
                    displayName,
                    avatarUrl
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.warn("Discord OAuth token verification interrupted", e);
            throw new InvalidOAuthTokenException();
        } catch (IOException e) {
            LOG.warn("Discord OAuth token verification failed", e);
            throw new InvalidOAuthTokenException();
        }
    }
}
