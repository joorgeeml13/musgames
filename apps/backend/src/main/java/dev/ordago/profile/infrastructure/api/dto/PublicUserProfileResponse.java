package dev.ordago.profile.infrastructure.api.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import dev.ordago.profile.domain.model.UserProfile;

@Schema(description = "Publicly visible player profile details")
public record PublicUserProfileResponse(
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

        @Schema(description = "Real-time online status", example = "true")
        boolean isOnline
) {
    public static PublicUserProfileResponse fromDomain(UserProfile domain) {
        return new PublicUserProfileResponse(
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
                domain.isOnline()
        );
    }
}
