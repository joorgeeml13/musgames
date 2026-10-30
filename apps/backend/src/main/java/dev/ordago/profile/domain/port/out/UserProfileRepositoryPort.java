package dev.ordago.profile.domain.port.out;

import java.util.Optional;
import java.util.UUID;
import dev.ordago.profile.domain.model.UserProfile;

public interface UserProfileRepositoryPort {

    UserProfile save(UserProfile userProfile);

    Optional<UserProfile> findById(UUID id);

    Optional<UserProfile> findByPlayerTag(String gameName, String tagLine);

    boolean existsByPlayerTag(String gameName, String tagLine);

    boolean existsById(UUID id);
}
