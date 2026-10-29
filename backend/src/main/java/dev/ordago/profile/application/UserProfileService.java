package dev.ordago.profile.application;

import java.util.Objects;
import java.util.UUID;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import dev.ordago.profile.domain.exception.RiotIdAlreadyTakenException;
import dev.ordago.profile.domain.exception.UserProfileNotFoundException;
import dev.ordago.profile.domain.model.RiotId;
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
        RiotId riotId = new RiotId(command.gameName(), command.tagLine());

        if (userProfileRepository.existsByRiotId(riotId.gameName(), riotId.tagLine())) {
            throw new RiotIdAlreadyTakenException(riotId.gameName(), riotId.tagLine());
        }

        UserProfile userProfile = UserProfile.createDefault(
                command.accountId(),
                riotId,
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
    public UserProfile getProfileByRiotId(String gameName, String tagLine) {
        RiotId riotId = new RiotId(gameName, tagLine);
        return userProfileRepository.findByRiotId(riotId.gameName(), riotId.tagLine())
                .orElseThrow(() -> new UserProfileNotFoundException(riotId.gameName(), riotId.tagLine()));
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
