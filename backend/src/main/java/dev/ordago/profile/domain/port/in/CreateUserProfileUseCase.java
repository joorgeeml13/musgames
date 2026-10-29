package dev.ordago.profile.domain.port.in;

import java.util.UUID;
import dev.ordago.profile.domain.model.UserProfile;

public interface CreateUserProfileUseCase {

    record CreateProfileCommand(
            UUID accountId,
            String gameName,
            String tagLine,
            String locale
    ) {}

    UserProfile createUserProfile(CreateProfileCommand command);
}
