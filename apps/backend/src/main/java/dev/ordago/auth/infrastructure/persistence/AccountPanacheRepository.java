package dev.ordago.auth.infrastructure.persistence;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import jakarta.enterprise.context.ApplicationScoped;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;

@ApplicationScoped
public class AccountPanacheRepository implements PanacheRepositoryBase<AccountEntity, UUID> {

    public Optional<AccountEntity> findByEmail(String email) {
        if (email == null) {
            return Optional.empty();
        }
        return find("email", email.trim().toLowerCase(Locale.ROOT)).firstResultOptional();
    }

    public boolean existsByEmail(String email) {
        if (email == null) {
            return false;
        }
        return count("email", email.trim().toLowerCase(Locale.ROOT)) > 0;
    }

    public Optional<AccountEntity> findByEmailVerificationToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return find("emailVerificationToken", token.trim()).firstResultOptional();
    }

    public Optional<AccountEntity> findByPasswordResetToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return find("passwordResetToken", token.trim()).firstResultOptional();
    }
}
