package dev.ordago.auth.infrastructure.persistence;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import jakarta.enterprise.context.ApplicationScoped;
import dev.ordago.auth.domain.model.Account;
import dev.ordago.auth.domain.port.out.AccountRepositoryPort;

@ApplicationScoped
public class AccountRepositoryAdapter implements AccountRepositoryPort {

    private final AccountPanacheRepository panacheRepository;

    public AccountRepositoryAdapter(AccountPanacheRepository panacheRepository) {
        this.panacheRepository = Objects.requireNonNull(panacheRepository, "AccountPanacheRepository cannot be null");
    }

    @Override
    public Account save(Account account) {
        AccountEntity entity = panacheRepository.findById(account.getId());
        if (entity == null) {
            entity = AccountEntity.fromDomain(account);
            panacheRepository.persist(entity);
        } else {
            entity.email = account.getEmail().value();
            entity.passwordHash = account.getPasswordHash().value();
            entity.isSystemAdmin = account.isSystemAdmin();
            entity.emailVerified = account.isEmailVerified();
            entity.emailVerificationToken = account.getEmailVerificationToken();
            entity.emailVerificationExpiresAt = account.getEmailVerificationExpiresAt();
            entity.passwordResetToken = account.getPasswordResetToken();
            entity.passwordResetExpiresAt = account.getPasswordResetExpiresAt();
            entity.failedLoginAttempts = account.getFailedLoginAttempts();
            entity.lockedUntil = account.getLockedUntil();
            entity.lastLoginAt = account.getLastLoginAt();
            entity.lastLoginIp = account.getLastLoginIp();
            entity.twoFactorEnabled = account.isTwoFactorEnabled();
            entity.twoFactorSecret = account.getTwoFactorSecret();
            entity.isBanned = account.isBanned();
            entity.bannedUntil = account.getBannedUntil();
            entity.banReason = account.getBanReason();
            entity.updatedAt = account.getUpdatedAt();
            entity.deletedAt = account.getDeletedAt();
        }
        return entity.toDomain();
    }

    @Override
    public Optional<Account> findById(UUID id) {
        return panacheRepository.findByIdOptional(id).map(AccountEntity::toDomain);
    }

    @Override
    public Optional<Account> findByEmail(String email) {
        return panacheRepository.findByEmail(email).map(AccountEntity::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return panacheRepository.existsByEmail(email);
    }
}
