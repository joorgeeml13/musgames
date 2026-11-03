package dev.ordago.profile.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class UserProfilePanacheRepositoryIT {

    @Inject
    UserProfilePanacheRepository repository;

    @Test
    @Transactional
    void shouldCalculateAvailableTagLineWithAdvisoryLockOnPostgreSql() {
        String tag = repository.calculateAvailableTagLine("LockTester");
        assertThat(tag).isEqualTo("1000");
    }

    @Test
    @Transactional
    void shouldReturnNextAvailableTagWhenFirstTagTaken() {
        UUID accountId = UUID.randomUUID();
        repository.getEntityManager().createNativeQuery(
                "INSERT INTO accounts (id, email, password_hash, is_system_admin, email_verified, failed_login_attempts, two_factor_enabled, is_banned, created_at, updated_at) " +
                "VALUES (?1, ?2, 'dummy_hash', false, false, 0, false, false, now(), now())"
        ).setParameter(1, accountId).setParameter(2, "slottester@ordago.dev").executeUpdate();

        UserProfileEntity entity = new UserProfileEntity();
        entity.id = accountId;
        entity.gameName = "SlotTester";
        entity.tagLine = "1000";
        entity.createdAt = Instant.now();
        entity.updatedAt = Instant.now();
        repository.persist(entity);

        String tag = repository.calculateAvailableTagLine("SlotTester");
        assertThat(tag).isEqualTo("1001");
    }

    @Test
    @Transactional
    void shouldCalculateAvailableTagLineCaseInsensitive() {
        UUID accountId = UUID.randomUUID();
        repository.getEntityManager().createNativeQuery(
                "INSERT INTO accounts (id, email, password_hash, is_system_admin, email_verified, failed_login_attempts, two_factor_enabled, is_banned, created_at, updated_at) " +
                "VALUES (?1, ?2, 'dummy_hash', false, false, 0, false, false, now(), now())"
        ).setParameter(1, accountId).setParameter(2, "casetester@ordago.dev").executeUpdate();

        UserProfileEntity entity = new UserProfileEntity();
        entity.id = accountId;
        entity.gameName = "CaseTester";
        entity.tagLine = "1000";
        entity.createdAt = Instant.now();
        entity.updatedAt = Instant.now();
        repository.persist(entity);

        String tag = repository.calculateAvailableTagLine("casetester");
        assertThat(tag).isEqualTo("1001");
    }
}
