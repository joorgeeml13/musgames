package dev.ordago.auth.infrastructure.api.dto;

import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import dev.ordago.auth.domain.model.Account;
import dev.ordago.profile.domain.model.UserProfile;

@Schema(description = "Detailed user and account information response")
public record UserInfoResponse(
        @Schema(description = "Unique account identifier", example = "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d")
        UUID id,

        @Schema(description = "Account email address", example = "player@ordago.dev")
        String email,

        @Schema(description = "Indicates whether the user has system administrator privileges", example = "false")
        boolean isSystemAdmin,

        @Schema(description = "Indicates whether the email has been verified", example = "true")
        boolean emailVerified,

        @Schema(description = "In-game name component of Riot ID", example = "ManoMaestro")
        String gameName,

        @Schema(description = "Tag line component of Riot ID", example = "ESP1")
        String tagLine,

        @Schema(description = "Formatted Riot ID (GameName#TAG)", example = "ManoMaestro#ESP1")
        String riotId,

        @Schema(description = "Custom display name", example = "Don Mus")
        String displayName,

        @Schema(description = "Current Elo matchmaking rating", example = "1200")
        int eloRating,

        @Schema(description = "Active unlocked player title", example = "Novato del Chinchón")
        String activeTitle,

        @Schema(description = "URL to player avatar image", example = "https://ordago.dev/avatars/default.png")
        String avatarUrl
) {
    public static UserInfoResponse of(Account account, UserProfile profile) {
        return new UserInfoResponse(
                account.getId(),
                account.getEmail().value(),
                account.isSystemAdmin(),
                account.isEmailVerified(),
                profile != null ? profile.getGameName() : null,
                profile != null ? profile.getTagLine() : null,
                profile != null ? profile.getRiotId().formatted() : null,
                profile != null ? profile.getDisplayName() : null,
                profile != null ? profile.getEloRating() : 1200,
                profile != null ? profile.getActiveTitle() : "Novato del Chinchón",
                profile != null ? profile.getAvatarUrl() : null
        );
    }
}
