package dev.ordago.profile.infrastructure.persistence;

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
        assertThat(tag).isNotBlank().matches("^[0-9]{4,5}$");
    }
}
