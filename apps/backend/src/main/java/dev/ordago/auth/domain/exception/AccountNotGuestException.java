package dev.ordago.auth.domain.exception;

import dev.ordago.shared.domain.exception.DomainException;

public class AccountNotGuestException extends DomainException {

    public AccountNotGuestException() {
        super("ACCOUNT_NOT_GUEST", "auth.account_not_guest", 400);
    }
}
