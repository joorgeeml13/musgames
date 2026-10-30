package dev.ordago.profile.domain.exception;

import dev.ordago.shared.domain.exception.DomainException;

public class InvalidPlayerTagException extends DomainException {
    public InvalidPlayerTagException(String details) {
        super("INVALID_PLAYER_TAG", "profile.invalid_player_tag", 400, java.util.List.of(details));
    }
}
