package dev.ordago.auth.domain.port.in;

import java.util.UUID;

public interface RevokeSessionUseCase {

    void revokeSession(UUID sessionId, String reason);

    void revokeSession(UUID accountId, UUID sessionId, String reason);

    void revokeOtherSessions(UUID accountId, UUID excludedSessionId, String reason);

    void revokeAllSessionsForAccount(UUID accountId, String reason);
}
