package dev.ordago.profile.infrastructure.api.dto;

import java.time.Instant;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import dev.ordago.profile.domain.model.UserProfile;

@Schema(description = "Full user profile representation for the authenticated user")
public record UserProfileResponse(
        @Schema(description = "User account ID", example = "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d")
        UUID id,

        @Schema(description = "Game name portion of player tag", example = "ManoMaestro")
        String gameName,

        @Schema(description = "Tag line portion of player tag", example = "ESP1")
        String tagLine,

        @Schema(description = "Formatted player tag (GameName#TAG)", example = "ManoMaestro#ESP1")
        String playerTag,

        @Schema(description = "Custom display name", example = "Don Mus")
        String displayName,

        @Schema(description = "Current Elo matchmaking rating", example = "1200")
        int eloRating,

        @Schema(description = "Total competitive games played", example = "42")
        int gamesPlayed,

        @Schema(description = "Total competitive games won", example = "28")
        int gamesWon,

        @Schema(description = "Active unlocked player title", example = "Novato del Chinchón")
        String activeTitle,

        @Schema(description = "Whether user is an early founder supporter", example = "false")
        boolean isFounder,

        @Schema(description = "Avatar image URL", example = "https://ordago.dev/avatars/default.png")
        String avatarUrl,

        @Schema(description = "Cosmetic avatar frame identifier", example = "frame_classic_gold")
        String avatarFrameId,

        @Schema(description = "Player biography or tagline", example = "¡Si tienes dudas, echa un órdago!")
        String bio,

        @Schema(description = "Preferred card back deck design", example = "fournier_classic_red")
        String preferredCardBackId,

        @Schema(description = "Preference for classic deck view rendering", example = "false")
        boolean forceClassicDeckView,

        @Schema(description = "Master sound effects volume percentage (0-100)", example = "80")
        int soundEffectsVolume,

        @Schema(description = "User interface locale preference", example = "es-ES")
        String locale,

        @Schema(description = "Real-time online status", example = "true")
        boolean isOnline,

        @Schema(description = "Timestamp when the user was last active", example = "2026-10-29T15:30:00Z")
        Instant lastSeenAt,

        @Schema(description = "Whether the user accepts direct match challenges from others", example = "true")
        boolean allowDirectChallenges,

        @Schema(description = "Profile creation timestamp", example = "2026-10-29T12:00:00Z")
        Instant createdAt,

        @Schema(description = "Profile last update timestamp", example = "2026-10-29T15:30:00Z")
        Instant updatedAt
) {
    public static UserProfileResponse fromDomain(UserProfile domain) {
        return new UserProfileResponse(
                domain.getId(),
                domain.getGameName(),
                domain.getTagLine(),
                domain.getPlayerTag().formatted(),
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
