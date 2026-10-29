package dev.ordago.auth.domain.exception;

import dev.ordago.shared.domain.exception.DomainException;

public class AccountBannedException extends DomainException {
    public AccountBannedException(String reason) {
        super("ACCOUNT_BANNED", "auth.account_banned", 403, reason != null ? reason : "Account suspended");
    }
}
