package dev.ordago.profile.domain.exception;

import java.util.UUID;
import dev.ordago.shared.domain.exception.DomainException;

public class UserProfileNotFoundException extends DomainException {
    public UserProfileNotFoundException(UUID userId) {
        super("PROFILE_NOT_FOUND", "profile.not_found", 404, String.valueOf(userId));
    }

    public UserProfileNotFoundException(String gameName, String tagLine) {
        super("PROFILE_NOT_FOUND", "profile.not_found", 404, gameName + "#" + tagLine);
    }
}
