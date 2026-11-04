package dev.ordago.auth.domain.port.in;

import java.time.Instant;

public interface PurgeExpiredSessionsUseCase {

    PurgeResult purgeRevokedOrExpiredSessions(int retentionDays);

    record PurgeResult(int purgedCount, Instant cutoff, int retentionDays) {}
}
