package dev.ordago.auth.infrastructure.api.dto;

import java.util.UUID;
import dev.ordago.auth.domain.model.Account;
import dev.ordago.profile.domain.model.UserProfile;

public record UserInfoResponse(
        UUID id,
        String email,
        boolean isSystemAdmin,
        boolean emailVerified,
        String gameName,
        String tagLine,
        String riotId,
        String displayName,
        int eloRating,
        String activeTitle,
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
