package dev.ordago.profile.infrastructure.api.dto;

import dev.ordago.profile.domain.model.UserProfile;

public record PublicUserProfileResponse(
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
        boolean isOnline
) {
    public static PublicUserProfileResponse fromDomain(UserProfile domain) {
        return new PublicUserProfileResponse(
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
                domain.isOnline()
        );
    }
}
