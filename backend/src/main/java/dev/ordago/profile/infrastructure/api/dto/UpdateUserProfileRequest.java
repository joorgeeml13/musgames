package dev.ordago.profile.infrastructure.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateUserProfileRequest(
        @Size(max = 48)
        String displayName,

        @Size(max = 255)
        String avatarUrl,

        @Size(max = 64)
        String avatarFrameId,

        @Size(max = 250)
        String bio,

        @Size(max = 64)
        String preferredCardBackId,

        Boolean forceClassicDeckView,

        @Min(0)
        @Max(100)
        Integer soundEffectsVolume,

        @Size(max = 8)
        String locale,

        Boolean allowDirectChallenges
) {}
