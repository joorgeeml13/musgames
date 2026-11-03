package dev.ordago.profile.infrastructure.persistence;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import jakarta.enterprise.context.ApplicationScoped;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import dev.ordago.profile.domain.exception.PlayerTagCapacityExceededException;

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

    public String calculateAvailableTagLine(String gameName) {
        String canonicalGameName = (gameName != null ? gameName.trim() : "").toLowerCase(Locale.ROOT);

        getEntityManager()
                .createNativeQuery("SELECT pg_advisory_xact_lock(hashtext(?1))")
                .setParameter(1, canonicalGameName)
                .getResultList();

        List<?> fourDigitSlot = getEntityManager()
                .createNativeQuery("""
                        SELECT s.val::text
                        FROM generate_series(1000, 9999) AS s(val)
                        WHERE NOT EXISTS (
                            SELECT 1
                            FROM user_profiles u
                            WHERE LOWER(u.game_name) = ?1
                              AND LOWER(u.tag_line) = s.val::text
                        )
                        LIMIT 1
                        """)
                .setParameter(1, canonicalGameName)
                .getResultList();

        if (!fourDigitSlot.isEmpty()) {
            return fourDigitSlot.getFirst().toString();
        }

        List<?> fiveDigitSlot = getEntityManager()
                .createNativeQuery("""
                        SELECT s.val::text
                        FROM generate_series(10000, 99999) AS s(val)
                        WHERE NOT EXISTS (
                            SELECT 1
                            FROM user_profiles u
                            WHERE LOWER(u.game_name) = ?1
                              AND LOWER(u.tag_line) = s.val::text
                        )
                        LIMIT 1
                        """)
                .setParameter(1, canonicalGameName)
                .getResultList();

        if (!fiveDigitSlot.isEmpty()) {
            return fiveDigitSlot.getFirst().toString();
        }

        throw new PlayerTagCapacityExceededException(gameName);
    }
}
