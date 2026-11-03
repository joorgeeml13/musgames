package dev.ordago.profile.infrastructure.persistence;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import jakarta.enterprise.context.ApplicationScoped;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import org.hibernate.Session;
import dev.ordago.profile.domain.exception.PlayerTagCapacityExceededException;

@ApplicationScoped
public class UserProfilePanacheRepository implements PanacheRepositoryBase<UserProfileEntity, UUID> {

    private volatile Boolean isPostgreSql;

    private boolean isPostgreSql() {
        if (isPostgreSql != null) {
            return isPostgreSql;
        }
        try {
            Session session = getEntityManager().unwrap(Session.class);
            isPostgreSql = session.doReturningWork(connection ->
                    connection.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT).contains("postgresql")
            );
            return isPostgreSql != null && isPostgreSql;
        } catch (Exception e) {
            return false;
        }
    }

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

        if (isPostgreSql()) {
            getEntityManager()
                    .createNativeQuery("SELECT pg_advisory_xact_lock(hashtext(?1))")
                    .setParameter(1, canonicalGameName)
                    .getResultList();
        }

        List<String> existingTags = getEntityManager()
                .createQuery("SELECT LOWER(p.tagLine) FROM UserProfileEntity p WHERE LOWER(p.gameName) = :gameName", String.class)
                .setParameter("gameName", canonicalGameName)
                .getResultList();
        Set<String> taken = new HashSet<>(existingTags);

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
