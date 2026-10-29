package dev.ordago.profile.domain.exception;

import dev.ordago.shared.domain.exception.DomainException;

public class RiotIdAlreadyTakenException extends DomainException {
    public RiotIdAlreadyTakenException(String gameName, String tagLine) {
        super("RIOT_ID_ALREADY_TAKEN", "profile.riot_id_taken", 409, gameName + "#" + tagLine);
    }
}
