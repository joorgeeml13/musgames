package dev.ordago.auth.domain.port.in;

import java.util.List;
import java.util.UUID;
import dev.ordago.auth.domain.model.UserSession;

public interface GetUserSessionsUseCase {

    List<UserSession> getActiveSessions(UUID accountId);
}
