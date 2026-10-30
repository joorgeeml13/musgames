package dev.ordago.profile.application;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import dev.ordago.profile.domain.exception.PlayerTagAlreadyTakenException;
import dev.ordago.profile.domain.exception.UserProfileNotFoundException;
import dev.ordago.profile.domain.model.UserProfile;
import dev.ordago.profile.domain.port.in.CreateUserProfileUseCase;
import dev.ordago.profile.domain.port.in.UpdateUserProfileUseCase;
import dev.ordago.profile.domain.port.out.UserProfileRepositoryPort;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserProfileServiceTest {

    private UserProfileService profileService;
    private InMemoryUserProfileRepository profileRepository;

    @BeforeEach
    void setUp() {
        profileRepository = new InMemoryUserProfileRepository();
        profileService = new UserProfileService(profileRepository);
    }

    @Test
    void shouldCreateDefaultProfileSuccessfully() {
        UUID accountId = UUID.randomUUID();
        CreateUserProfileUseCase.CreateProfileCommand cmd = new CreateUserProfileUseCase.CreateProfileCommand(
                accountId,
                "Chinchonero",
                "01",
                "es-ES"
        );

        UserProfile profile = profileService.createUserProfile(cmd);

        assertThat(profile).isNotNull();
        assertThat(profile.getId()).isEqualTo(accountId);
        assertThat(profile.getGameName()).isEqualTo("Chinchonero");
        assertThat(profile.getTagLine()).isEqualTo("01");
        assertThat(profile.getEloRating()).isEqualTo(1200);
        assertThat(profile.getActiveTitle()).isEqualTo("Novato del Chinchón");
        assertThat(profile.getDisplayName()).isEqualTo("Chinchonero");
    }

    @Test
    void shouldPreventDuplicatePlayerTagRegardlessOfCase() {
        UUID id1 = UUID.randomUUID();
        profileService.createUserProfile(new CreateUserProfileUseCase.CreateProfileCommand(
                id1,
                "Musero",
                "01",
                "es-ES"
        ));

        UUID id2 = UUID.randomUUID();
        assertThatThrownBy(() -> profileService.createUserProfile(new CreateUserProfileUseCase.CreateProfileCommand(
                id2,
                "musero",
                "01",
                "es-ES"
        ))).isInstanceOf(PlayerTagAlreadyTakenException.class);
    }

    @Test
    void shouldFindProfileByPlayerTagCaseInsensitive() {
        UUID id = UUID.randomUUID();
        profileService.createUserProfile(new CreateUserProfileUseCase.CreateProfileCommand(
                id,
                "MuseroRey",
                "ESP1",
                "es-ES"
        ));

        UserProfile found = profileService.getProfileByPlayerTag("muserorey", "esp1");

        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(id);
        assertThat(found.getGameName()).isEqualTo("MuseroRey");
        assertThat(found.getTagLine()).isEqualTo("ESP1");
    }

    @Test
    void shouldUpdateProfileDetails() {
        UUID accountId = UUID.randomUUID();
        profileService.createUserProfile(new CreateUserProfileUseCase.CreateProfileCommand(
                accountId,
                "ReyDeCopas",
                "77",
                "es-ES"
        ));

        UpdateUserProfileUseCase.UpdateProfileCommand updateCmd = new UpdateUserProfileUseCase.UpdateProfileCommand(
                accountId,
                "El Rey",
                "https://avatar.url/rey.png",
                "gold-frame",
                "Jugador veterano de mus.",
                "baraja-clasica",
                true,
                90,
                "es-ES",
                false
        );

        UserProfile updated = profileService.updateProfile(updateCmd);

        assertThat(updated.getDisplayName()).isEqualTo("El Rey");
        assertThat(updated.getAvatarUrl()).isEqualTo("https://avatar.url/rey.png");
        assertThat(updated.getAvatarFrameId()).isEqualTo("gold-frame");
        assertThat(updated.getBio()).isEqualTo("Jugador veterano de mus.");
        assertThat(updated.getPreferredCardBackId()).isEqualTo("baraja-clasica");
        assertThat(updated.isForceClassicDeckView()).isTrue();
        assertThat(updated.getSoundEffectsVolume()).isEqualTo(90);
        assertThat(updated.isAllowDirectChallenges()).isFalse();
    }

    @Test
    void shouldThrowWhenProfileNotFound() {
        UUID nonexistent = UUID.randomUUID();
        assertThatThrownBy(() -> profileService.getProfileById(nonexistent))
                .isInstanceOf(UserProfileNotFoundException.class);
        assertThatThrownBy(() -> profileService.getProfileByPlayerTag("Nonexistent", "00"))
                .isInstanceOf(UserProfileNotFoundException.class);
    }

    @Test
    void shouldRejectProfileUpdateForNonexistentAccount() {
        UUID nonexistent = UUID.randomUUID();
        UpdateUserProfileUseCase.UpdateProfileCommand updateCmd = new UpdateUserProfileUseCase.UpdateProfileCommand(
                nonexistent,
                "Ghost",
                null,
                null,
                null,
                null,
                false,
                80,
                "es-ES",
                true
        );

        assertThatThrownBy(() -> profileService.updateProfile(updateCmd))
                .isInstanceOf(UserProfileNotFoundException.class);
    }

    @Test
    void shouldRejectNullArguments() {
        assertThatThrownBy(() -> profileService.createUserProfile(null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> profileService.getProfileById(null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> profileService.updateProfile(null))
                .isInstanceOf(NullPointerException.class);
    }

    private static class InMemoryUserProfileRepository implements UserProfileRepositoryPort {
        private final Map<UUID, UserProfile> store = new HashMap<>();

        @Override
        public UserProfile save(UserProfile userProfile) {
            store.put(userProfile.getId(), userProfile);
            return userProfile;
        }

        @Override
        public Optional<UserProfile> findById(UUID id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public Optional<UserProfile> findByPlayerTag(String gameName, String tagLine) {
            return store.values().stream()
                    .filter(p -> p.getGameName().equalsIgnoreCase(gameName.trim().toLowerCase(Locale.ROOT))
                            && p.getTagLine().equalsIgnoreCase(tagLine.trim().toLowerCase(Locale.ROOT)))
                    .findFirst();
        }

        @Override
        public boolean existsByPlayerTag(String gameName, String tagLine) {
            return findByPlayerTag(gameName, tagLine).isPresent();
        }

        @Override
        public boolean existsById(UUID id) {
            return store.containsKey(id);
        }
    }
}
