package dev.ordago.profile.infrastructure.api.dto;

import java.time.Instant;
import java.util.UUID;
import dev.ordago.profile.domain.model.UserProfile;

public record UserProfileResponse(
        UUID id,
        String gameName,
        String tagLine,
        String riotId,
        String displayName,
        int eloRating,
        int gamesPlayed,
        int gamesWon,
        String activeTitle,
        boolean isFounder,
        String avatarUrl,
        String avatarFrameId,
        String bio,
        String preferredCardBackId,
        boolean forceClassicDeckView,
        int soundEffectsVolume,
        String locale,
        boolean isOnline,
        Instant lastSeenAt,
        boolean allowDirectChallenges,
        Instant createdAt,
        Instant updatedAt
) {
    public static UserProfileResponse fromDomain(UserProfile domain) {
        return new UserProfileResponse(
                domain.getId(),
                domain.getGameName(),
                domain.getTagLine(),
                domain.getRiotId().formatted(),
                domain.getDisplayName(),
                domain.getEloRating(),
                domain.getGamesPlayed(),
                domain.getGamesWon(),
                domain.getActiveTitle(),
                domain.isFounder(),
                domain.getAvatarUrl(),
                domain.getAvatarFrameId(),
                domain.getBio(),
                domain.getPreferredCardBackId(),
                domain.isForceClassicDeckView(),
                domain.getSoundEffectsVolume(),
                domain.getLocale(),
                domain.isOnline(),
                domain.getLastSeenAt(),
                domain.isAllowDirectChallenges(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }
}
