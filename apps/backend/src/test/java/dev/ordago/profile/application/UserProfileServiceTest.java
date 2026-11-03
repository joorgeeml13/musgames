package dev.ordago.profile.application;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import dev.ordago.profile.domain.exception.PlayerTagAlreadyTakenException;
import dev.ordago.profile.domain.exception.PlayerTagCapacityExceededException;
import dev.ordago.profile.domain.exception.UserProfileNotFoundException;
import dev.ordago.profile.domain.model.PlayerTag;
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
        assertThatThrownBy(() -> profileService.calculateAvailableTagLine(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldAssignAvailableTagLineWhenTagLineIsNull() {
        UUID accountId = UUID.randomUUID();
        CreateUserProfileUseCase.CreateProfileCommand cmd = new CreateUserProfileUseCase.CreateProfileCommand(
                accountId,
                "AutoMusero",
                null,
                "es-ES"
        );

        UserProfile profile = profileService.createUserProfile(cmd);

        assertThat(profile).isNotNull();
        assertThat(profile.getTagLine()).isNotBlank().matches("^[0-9]{4,5}$");
        assertThat(profile.getGameName()).isEqualTo("AutoMusero");
    }

    @Test
    void shouldAssignAvailableTagLineWhenTagLineIsBlank() {
        UUID accountId = UUID.randomUUID();
        CreateUserProfileUseCase.CreateProfileCommand cmd = new CreateUserProfileUseCase.CreateProfileCommand(
                accountId,
                "AutoMuseroBlank",
                "   ",
                "es-ES"
        );

        UserProfile profile = profileService.createUserProfile(cmd);

        assertThat(profile).isNotNull();
        assertThat(profile.getTagLine()).isNotBlank().matches("^[0-9]{4,5}$");
    }

    @Test
    void shouldCalculateAvailableTagLineAvoidingTakenTags() {
        String gameName = "PopularPlayer";
        // Pre-populate some tags
        profileRepository.save(UserProfile.createDefault(UUID.randomUUID(), new PlayerTag(gameName, "1111"), "es-ES"));
        profileRepository.save(UserProfile.createDefault(UUID.randomUUID(), new PlayerTag(gameName, "2222"), "es-ES"));

        String tagLine = profileService.calculateAvailableTagLine(gameName);

        assertThat(tagLine).isNotNull();
        assertThat(tagLine).isNotEqualTo("1111");
        assertThat(tagLine).isNotEqualTo("2222");
        assertThat(tagLine).matches("^[0-9]{4,5}$");
    }

    @Test
    void shouldFallbackToFiveDigitsWhenAllFourDigitsTaken() {
        String gameName = "CrowdedPlayer";
        // Fill all 9000 4-digit tags (1000 to 9999)
        for (int i = 1000; i <= 9999; i++) {
            profileRepository.save(UserProfile.createDefault(UUID.randomUUID(), new PlayerTag(gameName, String.valueOf(i)), "es-ES"));
        }

        String tagLine = profileService.calculateAvailableTagLine(gameName);

        assertThat(tagLine).isNotNull();
        int tagValue = Integer.parseInt(tagLine);
        assertThat(tagValue).isBetween(10000, 99999);
    }

    @Test
    void shouldThrowPlayerTagCapacityExceededExceptionWhenCapacityExhausted() {
        UserProfileRepositoryPort fullRepo = new InMemoryUserProfileRepository() {
            @Override
            public String calculateAvailableTagLine(String gameName) {
                throw new PlayerTagCapacityExceededException(gameName);
            }
        };
        UserProfileService serviceWithFullRepo = new UserProfileService(fullRepo);

        assertThatThrownBy(() -> serviceWithFullRepo.calculateAvailableTagLine("MaxedUser"))
                .isInstanceOf(PlayerTagCapacityExceededException.class)
                .hasMessage("profile.player_tag_capacity_exceeded");
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

        @Override
        public String calculateAvailableTagLine(String gameName) {
            String canonicalGameName = (gameName != null ? gameName.trim() : "").toLowerCase(Locale.ROOT);
            Set<String> taken = store.values().stream()
                    .filter(p -> p.getGameName().trim().toLowerCase(Locale.ROOT).equals(canonicalGameName))
                    .map(p -> p.getTagLine().trim().toLowerCase(Locale.ROOT))
                    .collect(Collectors.toSet());

            for (int i = 0; i < 50; i++) {
                int candidate = ThreadLocalRandom.current().nextInt(1000, 10000);
                String tag = String.valueOf(candidate);
                if (!taken.contains(tag)) {
                    return tag;
                }
            }

            int offset4 = ThreadLocalRandom.current().nextInt(0, 9000);
            for (int i = 0; i < 9000; i++) {
                int candidate = 1000 + ((offset4 + i) % 9000);
                String tag = String.valueOf(candidate);
                if (!taken.contains(tag)) {
                    return tag;
                }
            }

            for (int i = 0; i < 50; i++) {
                int candidate = ThreadLocalRandom.current().nextInt(10000, 100000);
                String tag = String.valueOf(candidate);
                if (!taken.contains(tag)) {
                    return tag;
                }
            }

            int offset5 = ThreadLocalRandom.current().nextInt(0, 90000);
            for (int i = 0; i < 90000; i++) {
                int candidate = 10000 + ((offset5 + i) % 90000);
                String tag = String.valueOf(candidate);
                if (!taken.contains(tag)) {
                    return tag;
                }
            }

            throw new PlayerTagCapacityExceededException(gameName);
        }
    }
}
