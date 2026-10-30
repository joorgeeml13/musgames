package dev.ordago.profile.domain.exception;

import dev.ordago.shared.domain.exception.DomainException;

public class PlayerTagAlreadyTakenException extends DomainException {
    public PlayerTagAlreadyTakenException(String gameName, String tagLine) {
        super("PLAYER_TAG_ALREADY_TAKEN", "profile.player_tag_taken", 409, gameName + "#" + tagLine);
    }
}
