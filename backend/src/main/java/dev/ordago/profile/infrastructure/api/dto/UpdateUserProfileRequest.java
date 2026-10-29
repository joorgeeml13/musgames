package dev.ordago.profile.infrastructure.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "User profile update request payload")
public record UpdateUserProfileRequest(
        @Schema(description = "Custom display name (max 48 chars)", example = "Don Mus", maxLength = 48)
        @Size(max = 48)
        String displayName,

        @Schema(description = "URL pointing to the new avatar image", example = "https://ordago.dev/avatars/custom.png", maxLength = 255)
        @Size(max = 255)
        String avatarUrl,

        @Schema(description = "Identifier for unlocked cosmetic avatar frame", example = "frame_classic_gold", maxLength = 64)
        @Size(max = 64)
        String avatarFrameId,

        @Schema(description = "Short biography (max 250 chars)", example = "¡Si tienes dudas, echa un órdago!", maxLength = 250)
        @Size(max = 250)
        String bio,

        @Schema(description = "Preferred card back deck design", example = "fournier_classic_red", maxLength = 64)
        @Size(max = 64)
        String preferredCardBackId,

        @Schema(description = "Force classic deck rendering view", example = "false")
        Boolean forceClassicDeckView,

        @Schema(description = "Sound effects volume (0-100)", example = "80", minimum = "0", maximum = "100")
        @Min(0)
        @Max(100)
        Integer soundEffectsVolume,

        @Schema(description = "Preferred UI locale code (e.g. es-ES, en-US)", example = "es-ES", maxLength = 8)
        @Size(max = 8)
        String locale,

        @Schema(description = "Accept direct challenges from other players", example = "true")
        Boolean allowDirectChallenges
) {}
