package dev.ordago.auth.domain.exception;

import java.time.Instant;
import dev.ordago.shared.domain.exception.DomainException;

public class AccountLockedException extends DomainException {
    public AccountLockedException(Instant lockedUntil) {
        super("ACCOUNT_LOCKED", "auth.account_locked", 423, lockedUntil != null ? lockedUntil.toString() : "");
    }
}
