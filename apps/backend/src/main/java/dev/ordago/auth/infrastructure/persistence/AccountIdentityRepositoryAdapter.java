package dev.ordago.auth.infrastructure.persistence;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import jakarta.enterprise.context.ApplicationScoped;
import dev.ordago.auth.domain.model.AccountIdentity;
import dev.ordago.auth.domain.model.OAuthProvider;
import dev.ordago.auth.domain.port.out.AccountIdentityRepositoryPort;

@ApplicationScoped
public class AccountIdentityRepositoryAdapter implements AccountIdentityRepositoryPort {

    private final AccountIdentityPanacheRepository panacheRepository;

    public AccountIdentityRepositoryAdapter(AccountIdentityPanacheRepository panacheRepository) {
        this.panacheRepository = Objects.requireNonNull(panacheRepository, "AccountIdentityPanacheRepository cannot be null");
    }

    @Override
    public Optional<AccountIdentity> findByProviderAndSubject(OAuthProvider provider, String providerSubject) {
        Objects.requireNonNull(provider, "provider cannot be null");
        Objects.requireNonNull(providerSubject, "providerSubject cannot be null");
        return panacheRepository.findByProviderAndSubject(provider.name(), providerSubject)
                .map(AccountIdentityEntity::toDomain);
    }

    @Override
    public List<AccountIdentity> findByAccountId(UUID accountId) {
        Objects.requireNonNull(accountId, "accountId cannot be null");
        return panacheRepository.findByAccountId(accountId)
                .stream()
                .map(AccountIdentityEntity::toDomain)
                .toList();
    }

    @Override
    public AccountIdentity save(AccountIdentity identity) {
        Objects.requireNonNull(identity, "identity cannot be null");
        AccountIdentityEntity entity = panacheRepository.findById(identity.id());
        if (entity == null) {
            entity = AccountIdentityEntity.fromDomain(identity);
            panacheRepository.persist(entity);
        } else {
            entity.provider = identity.provider().name();
            entity.providerSubject = identity.providerSubject();
            entity.email = identity.email();
            entity.updatedAt = identity.updatedAt();
        }
        return entity.toDomain();
    }
}
