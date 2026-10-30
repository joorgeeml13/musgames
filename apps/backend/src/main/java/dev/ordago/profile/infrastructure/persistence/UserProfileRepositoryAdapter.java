package dev.ordago.profile.infrastructure.persistence;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import jakarta.enterprise.context.ApplicationScoped;
import dev.ordago.profile.domain.model.UserProfile;
import dev.ordago.profile.domain.port.out.UserProfileRepositoryPort;

@ApplicationScoped
public class UserProfileRepositoryAdapter implements UserProfileRepositoryPort {

    private final UserProfilePanacheRepository panacheRepository;

    public UserProfileRepositoryAdapter(UserProfilePanacheRepository panacheRepository) {
        this.panacheRepository = Objects.requireNonNull(panacheRepository, "UserProfilePanacheRepository cannot be null");
    }

    @Override
    public UserProfile save(UserProfile userProfile) {
        UserProfileEntity entity = panacheRepository.findById(userProfile.getId());
        if (entity == null) {
            entity = UserProfileEntity.fromDomain(userProfile);
            panacheRepository.persist(entity);
        } else {
            entity.gameName = userProfile.getGameName();
            entity.tagLine = userProfile.getTagLine();
            entity.displayName = userProfile.getDisplayName();
            entity.eloRating = userProfile.getEloRating();
            entity.gamesPlayed = userProfile.getGamesPlayed();
            entity.gamesWon = userProfile.getGamesWon();
            entity.activeTitle = userProfile.getActiveTitle();
            entity.isFounder = userProfile.isFounder();
            entity.avatarUrl = userProfile.getAvatarUrl();
            entity.avatarFrameId = userProfile.getAvatarFrameId();
            entity.bio = userProfile.getBio();
            entity.preferredCardBackId = userProfile.getPreferredCardBackId();
            entity.forceClassicDeckView = userProfile.isForceClassicDeckView();
            entity.soundEffectsVolume = userProfile.getSoundEffectsVolume();
            entity.locale = userProfile.getLocale();
            entity.isOnline = userProfile.isOnline();
            entity.lastSeenAt = userProfile.getLastSeenAt();
            entity.allowDirectChallenges = userProfile.isAllowDirectChallenges();
            entity.updatedAt = userProfile.getUpdatedAt();
        }
        return entity.toDomain();
    }

    @Override
    public Optional<UserProfile> findById(UUID id) {
        return panacheRepository.findByIdOptional(id).map(UserProfileEntity::toDomain);
    }

    @Override
    public Optional<UserProfile> findByPlayerTag(String gameName, String tagLine) {
        return panacheRepository.findByPlayerTag(gameName, tagLine).map(UserProfileEntity::toDomain);
    }

    @Override
    public boolean existsByPlayerTag(String gameName, String tagLine) {
        return panacheRepository.existsByPlayerTag(gameName, tagLine);
    }

    @Override
    public boolean existsById(UUID id) {
        return panacheRepository.count("id", id) > 0;
    }
}
