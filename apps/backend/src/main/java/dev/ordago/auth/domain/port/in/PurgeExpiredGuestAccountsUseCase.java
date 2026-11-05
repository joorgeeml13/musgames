package dev.ordago.auth.domain.port.in;

import java.time.Instant;

public interface PurgeExpiredGuestAccountsUseCase {

    record PurgeGuestsResult(long purgedCount, Instant cutoff, int retentionDays) {}

    PurgeGuestsResult purgeExpiredGuestAccounts(int retentionDays);
}
