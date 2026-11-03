package dev.ordago.profile.domain.exception;

import dev.ordago.shared.domain.exception.DomainException;

public class PlayerTagCapacityExceededException extends DomainException {
    public PlayerTagCapacityExceededException(String gameName) {
        super("PLAYER_TAG_CAPACITY_EXCEEDED", "profile.player_tag_capacity_exceeded", 409, gameName);
    }
}
