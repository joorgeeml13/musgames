package dev.ordago.profile.domain.port.in;

import java.util.UUID;
import dev.ordago.profile.domain.model.UserProfile;

public interface UpdateUserProfileUseCase {

    record UpdateProfileCommand(
            UUID accountId,
            String displayName,
            String avatarUrl,
            String avatarFrameId,
            String bio,
            String preferredCardBackId,
            Boolean forceClassicDeckView,
            Integer soundEffectsVolume,
            String locale,
            Boolean allowDirectChallenges
    ) {}

    UserProfile updateProfile(UpdateProfileCommand command);
}
