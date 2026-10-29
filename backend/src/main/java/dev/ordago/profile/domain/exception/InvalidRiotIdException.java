package dev.ordago.profile.domain.exception;

import dev.ordago.shared.domain.exception.DomainException;

public class InvalidRiotIdException extends DomainException {
    public InvalidRiotIdException(String details) {
        super("INVALID_RIOT_ID", "profile.invalid_riot_id", 400, java.util.List.of(details));
    }
}
