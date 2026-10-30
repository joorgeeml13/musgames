package dev.ordago.profile.infrastructure.persistence;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import jakarta.enterprise.context.ApplicationScoped;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;

@ApplicationScoped
public class UserProfilePanacheRepository implements PanacheRepositoryBase<UserProfileEntity, UUID> {

    public Optional<UserProfileEntity> findByPlayerTag(String gameName, String tagLine) {
        if (gameName == null || tagLine == null) {
            return Optional.empty();
        }
        return find(
                "LOWER(gameName) = LOWER(?1) AND LOWER(tagLine) = LOWER(?2)",
                gameName.trim().toLowerCase(Locale.ROOT),
                tagLine.trim().toLowerCase(Locale.ROOT)
        ).firstResultOptional();
    }

    public boolean existsByPlayerTag(String gameName, String tagLine) {
        if (gameName == null || tagLine == null) {
            return false;
        }
        return count(
                "LOWER(gameName) = LOWER(?1) AND LOWER(tagLine) = LOWER(?2)",
                gameName.trim().toLowerCase(Locale.ROOT),
                tagLine.trim().toLowerCase(Locale.ROOT)
        ) > 0;
    }
}
