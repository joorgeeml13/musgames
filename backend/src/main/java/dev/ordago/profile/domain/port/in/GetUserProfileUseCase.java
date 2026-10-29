package dev.ordago.profile.domain.port.in;

import java.util.UUID;
import dev.ordago.profile.domain.model.UserProfile;

public interface GetUserProfileUseCase {

    UserProfile getProfileById(UUID accountId);

    UserProfile getProfileByRiotId(String gameName, String tagLine);
}
