package dev.ordago.profile.application;

import java.util.Objects;
import java.util.UUID;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import dev.ordago.profile.domain.exception.PlayerTagAlreadyTakenException;
import dev.ordago.profile.domain.exception.UserProfileNotFoundException;
import dev.ordago.profile.domain.model.PlayerTag;
import dev.ordago.profile.domain.model.UserProfile;
import dev.ordago.profile.domain.port.in.CreateUserProfileUseCase;
import dev.ordago.profile.domain.port.in.GetUserProfileUseCase;
import dev.ordago.profile.domain.port.in.UpdateUserProfileUseCase;
import dev.ordago.profile.domain.port.out.UserProfileRepositoryPort;

@ApplicationScoped
public class UserProfileService implements CreateUserProfileUseCase, GetUserProfileUseCase, UpdateUserProfileUseCase {

    private final UserProfileRepositoryPort userProfileRepository;

    public UserProfileService(UserProfileRepositoryPort userProfileRepository) {
        this.userProfileRepository = Objects.requireNonNull(userProfileRepository, "UserProfileRepositoryPort cannot be null");
    }

    @Override
    @Transactional
    public UserProfile createUserProfile(CreateProfileCommand command) {
        Objects.requireNonNull(command, "CreateProfileCommand cannot be null");
        PlayerTag playerTag = new PlayerTag(command.gameName(), command.tagLine());

        if (userProfileRepository.existsByPlayerTag(playerTag.gameName(), playerTag.tagLine())) {
            throw new PlayerTagAlreadyTakenException(playerTag.gameName(), playerTag.tagLine());
        }

        UserProfile userProfile = UserProfile.createDefault(
                command.accountId(),
                playerTag,
                command.locale()
        );

        return userProfileRepository.save(userProfile);
    }

    @Override
    @Transactional
    public UserProfile getProfileById(UUID accountId) {
        Objects.requireNonNull(accountId, "accountId cannot be null");
        return userProfileRepository.findById(accountId)
                .orElseThrow(() -> new UserProfileNotFoundException(accountId));
    }

    @Override
    @Transactional
    public UserProfile getProfileByPlayerTag(String gameName, String tagLine) {
        PlayerTag playerTag = new PlayerTag(gameName, tagLine);
        return userProfileRepository.findByPlayerTag(playerTag.gameName(), playerTag.tagLine())
                .orElseThrow(() -> new UserProfileNotFoundException(playerTag.gameName(), playerTag.tagLine()));
    }

    @Override
    @Transactional
    public UserProfile updateProfile(UpdateProfileCommand command) {
        Objects.requireNonNull(command, "UpdateProfileCommand cannot be null");
        UserProfile profile = getProfileById(command.accountId());

        profile.updateDetails(
                command.displayName(),
                command.avatarUrl(),
                command.avatarFrameId(),
                command.bio(),
                command.preferredCardBackId(),
                command.forceClassicDeckView(),
                command.soundEffectsVolume(),
                command.locale(),
                command.allowDirectChallenges()
        );

        return userProfileRepository.save(profile);
    }
}
