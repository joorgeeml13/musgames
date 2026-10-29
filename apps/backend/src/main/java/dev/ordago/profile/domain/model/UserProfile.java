package dev.ordago.profile.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class UserProfile {
    private final UUID id;
    private RiotId riotId;
    private String displayName;
    private int eloRating;
    private int gamesPlayed;
    private int gamesWon;
    private String activeTitle;
    private boolean isFounder;
    private String avatarUrl;
    private String avatarFrameId;
    private String bio;
    private String preferredCardBackId;
    private boolean forceClassicDeckView;
    private int soundEffectsVolume;
    private String locale;
    private boolean isOnline;
    private Instant lastSeenAt;
    private boolean allowDirectChallenges;
    private final Instant createdAt;
    private Instant updatedAt;

    public UserProfile(
            UUID id,
            RiotId riotId,
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
        this.id = Objects.requireNonNull(id, "User ID cannot be null");
        this.riotId = Objects.requireNonNull(riotId, "RiotId cannot be null");
        this.displayName = displayName != null ? displayName : riotId.gameName();
        this.eloRating = eloRating;
        this.gamesPlayed = gamesPlayed;
        this.gamesWon = gamesWon;
        this.activeTitle = activeTitle != null ? activeTitle : "Novato del Chinchón";
        this.isFounder = isFounder;
        this.avatarUrl = avatarUrl;
        this.avatarFrameId = avatarFrameId;
        this.bio = bio;
        this.preferredCardBackId = preferredCardBackId;
        this.forceClassicDeckView = forceClassicDeckView;
        this.soundEffectsVolume = soundEffectsVolume;
        this.locale = locale != null ? locale : "es-ES";
        this.isOnline = isOnline;
        this.lastSeenAt = lastSeenAt;
        this.allowDirectChallenges = allowDirectChallenges;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
    }

    public static UserProfile createDefault(UUID id, RiotId riotId, String locale) {
        Instant now = Instant.now();
        return new UserProfile(
                id,
                riotId,
                riotId.gameName(),
                1200,
                0,
                0,
                "Novato del Chinchón",
                false,
                null,
                null,
                null,
                null,
                false,
                80,
                locale != null ? locale : "es-ES",
                false,
                null,
                true,
                now,
                now
        );
    }

    public void updateDetails(
            String displayName,
            String avatarUrl,
            String avatarFrameId,
            String bio,
            String preferredCardBackId,
            Boolean forceClassicDeckView,
            Integer soundEffectsVolume,
            String locale,
            Boolean allowDirectChallenges
    ) {
        if (displayName != null && !displayName.isBlank()) {
            this.displayName = displayName.trim();
        }
        if (avatarUrl != null) {
            this.avatarUrl = avatarUrl.isBlank() ? null : avatarUrl.trim();
        }
        if (avatarFrameId != null) {
            this.avatarFrameId = avatarFrameId.isBlank() ? null : avatarFrameId.trim();
        }
        if (bio != null) {
            this.bio = bio.length() > 250 ? bio.substring(0, 250) : bio;
        }
        if (preferredCardBackId != null) {
            this.preferredCardBackId = preferredCardBackId.isBlank() ? null : preferredCardBackId.trim();
        }
        if (forceClassicDeckView != null) {
            this.forceClassicDeckView = forceClassicDeckView;
        }
        if (soundEffectsVolume != null) {
            this.soundEffectsVolume = Math.max(0, Math.min(100, soundEffectsVolume));
        }
        if (locale != null && !locale.isBlank()) {
            this.locale = locale.trim();
        }
        if (allowDirectChallenges != null) {
            this.allowDirectChallenges = allowDirectChallenges;
        }
        this.updatedAt = Instant.now();
    }

    public void updateOnlineStatus(boolean online) {
        this.isOnline = online;
        this.lastSeenAt = Instant.now();
        this.updatedAt = this.lastSeenAt;
    }

    public void recordMatchResult(boolean won, int newElo) {
        this.gamesPlayed++;
        if (won) {
            this.gamesWon++;
        }
        this.eloRating = Math.max(0, newElo);
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public RiotId getRiotId() {
        return riotId;
    }

    public String getGameName() {
        return riotId.gameName();
    }

    public String getTagLine() {
        return riotId.tagLine();
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getEloRating() {
        return eloRating;
    }

    public int getGamesPlayed() {
        return gamesPlayed;
    }

    public int getGamesWon() {
        return gamesWon;
    }

    public String getActiveTitle() {
        return activeTitle;
    }

    public boolean isFounder() {
        return isFounder;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getAvatarFrameId() {
        return avatarFrameId;
    }

    public String getBio() {
        return bio;
    }

    public String getPreferredCardBackId() {
        return preferredCardBackId;
    }

    public boolean isForceClassicDeckView() {
        return forceClassicDeckView;
    }

    public int getSoundEffectsVolume() {
        return soundEffectsVolume;
    }

    public String getLocale() {
        return locale;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public boolean isAllowDirectChallenges() {
        return allowDirectChallenges;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
