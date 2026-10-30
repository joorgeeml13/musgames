package dev.ordago.profile.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import dev.ordago.profile.domain.model.PlayerTag;
import dev.ordago.profile.domain.model.UserProfile;

@Entity
@Table(
        name = "user_profiles",
        indexes = {
                @Index(name = "idx_user_profiles_player_tag", columnList = "game_name, tag_line")
        }
)
public class UserProfileEntity extends PanacheEntityBase {

    @Id
    @Column(name = "id", nullable = false)
    public UUID id;

    @Column(name = "game_name", nullable = false, length = 24)
    public String gameName;

    @Column(name = "tag_line", nullable = false, length = 6)
    public String tagLine;

    @Column(name = "display_name", length = 48)
    public String displayName;

    @Column(name = "elo_rating", nullable = false)
    public int eloRating = 1200;

    @Column(name = "games_played", nullable = false)
    public int gamesPlayed = 0;

    @Column(name = "games_won", nullable = false)
    public int gamesWon = 0;

    @Column(name = "active_title", length = 64)
    public String activeTitle = "Novato del Chinchón";

    @Column(name = "is_founder", nullable = false)
    public boolean isFounder = false;

    @Column(name = "avatar_url", length = 255)
    public String avatarUrl;

    @Column(name = "avatar_frame_id", length = 64)
    public String avatarFrameId;

    @Column(name = "bio", length = 250)
    public String bio;

    @Column(name = "preferred_card_back_id", length = 64)
    public String preferredCardBackId;

    @Column(name = "force_classic_deck_view", nullable = false)
    public boolean forceClassicDeckView = false;

    @Column(name = "sound_effects_volume", nullable = false)
    public int soundEffectsVolume = 80;

    @Column(name = "locale", nullable = false, length = 8)
    public String locale = "es-ES";

    @Column(name = "is_online", nullable = false)
    public boolean isOnline = false;

    @Column(name = "last_seen_at")
    public Instant lastSeenAt;

    @Column(name = "allow_direct_challenges", nullable = false)
    public boolean allowDirectChallenges = true;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;

    public UserProfile toDomain() {
        return new UserProfile(
                this.id,
                new PlayerTag(this.gameName, this.tagLine),
                this.displayName,
                this.eloRating,
                this.gamesPlayed,
                this.gamesWon,
                this.activeTitle,
                this.isFounder,
                this.avatarUrl,
                this.avatarFrameId,
                this.bio,
                this.preferredCardBackId,
                this.forceClassicDeckView,
                this.soundEffectsVolume,
                this.locale,
                this.isOnline,
                this.lastSeenAt,
                this.allowDirectChallenges,
                this.createdAt,
                this.updatedAt
        );
    }

    public static UserProfileEntity fromDomain(UserProfile domain) {
        UserProfileEntity entity = new UserProfileEntity();
        entity.id = domain.getId();
        entity.gameName = domain.getGameName();
        entity.tagLine = domain.getTagLine();
        entity.displayName = domain.getDisplayName();
        entity.eloRating = domain.getEloRating();
        entity.gamesPlayed = domain.getGamesPlayed();
        entity.gamesWon = domain.getGamesWon();
        entity.activeTitle = domain.getActiveTitle();
        entity.isFounder = domain.isFounder();
        entity.avatarUrl = domain.getAvatarUrl();
        entity.avatarFrameId = domain.getAvatarFrameId();
        entity.bio = domain.getBio();
        entity.preferredCardBackId = domain.getPreferredCardBackId();
        entity.forceClassicDeckView = domain.isForceClassicDeckView();
        entity.soundEffectsVolume = domain.getSoundEffectsVolume();
        entity.locale = domain.getLocale();
        entity.isOnline = domain.isOnline();
        entity.lastSeenAt = domain.getLastSeenAt();
        entity.allowDirectChallenges = domain.isAllowDirectChallenges();
        entity.createdAt = domain.getCreatedAt();
        entity.updatedAt = domain.getUpdatedAt();
        return entity;
    }
}
