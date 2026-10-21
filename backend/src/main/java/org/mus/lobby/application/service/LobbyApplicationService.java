package org.mus.lobby.application.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.mus.lobby.application.port.input.ProcessLobbyEventUseCase;
import org.mus.lobby.domain.port.output.LobbyBroadcasterPort;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@ApplicationScoped
public class LobbyApplicationService implements ProcessLobbyEventUseCase {

    private static final Logger LOG = Logger.getLogger(LobbyApplicationService.class);
    private static final int MAX_MESSAGE_LENGTH = 4096;
    private static final int MAX_MESSAGES_PER_WINDOW = 20;
    private static final long WINDOW_MS = 1000L;

    @Inject
    LobbyBroadcasterPort broadcaster;

    private final ConcurrentHashMap<String, RateLimitBucket> rateLimiters = new ConcurrentHashMap<>();

    private static class RateLimitBucket {
        volatile long windowStart = System.currentTimeMillis();
        final AtomicInteger count = new AtomicInteger(0);

        boolean allowRequest() {
            long now = System.currentTimeMillis();
            if (now - windowStart > WINDOW_MS) {
                synchronized (this) {
                    if (now - windowStart > WINDOW_MS) {
                        windowStart = now;
                        count.set(0);
                    }
                }
            }
            return count.incrementAndGet() <= MAX_MESSAGES_PER_WINDOW;
        }
    }

    @Override
    public Uni<Void> processAndBroadcast(String connectionId, String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank()) {
            return Uni.createFrom().voidItem();
        }

        // Rate limiting check per connection to prevent DoS broadcast storms
        if (connectionId != null) {
            RateLimitBucket bucket = rateLimiters.computeIfAbsent(connectionId, k -> new RateLimitBucket());
            if (!bucket.allowRequest()) {
                LOG.warnf("Rate limit exceeded for connection %s. Dropping message.", sanitizeLog(connectionId));
                return Uni.createFrom().voidItem();
            }
        }

        if (rawMessage.length() > MAX_MESSAGE_LENGTH) {
            LOG.warnf("Lobby payload size limit exceeded by connection %s (size: %d bytes)",
                    sanitizeLog(connectionId), rawMessage.length());
            return Uni.createFrom().voidItem();
        }

        LOG.infof("Lobby message from %s: %s", sanitizeLog(connectionId), sanitizeLog(rawMessage));
        return broadcaster.broadcast(rawMessage);
    }

    private String sanitizeLog(String input) {
        if (input == null) {
            return "null";
        }
        // CWE-117: Strip CRLF, tabs, ANSI terminal escape sequences and control characters
        String clean = input.replaceAll("[\\r\\n\\t\\u001b\\p{Cntrl}]", "_");
        // Prevent log flooding / disk exhaustion
        return clean.length() > 120 ? clean.substring(0, 120) + "... [truncated]" : clean;
    }
}
