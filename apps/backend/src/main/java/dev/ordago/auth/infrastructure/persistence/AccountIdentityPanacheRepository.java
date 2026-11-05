package dev.ordago.auth.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AccountIdentityPanacheRepository implements PanacheRepositoryBase<AccountIdentityEntity, UUID> {

    public Optional<AccountIdentityEntity> findByProviderAndSubject(String provider, String providerSubject) {
        return find("provider = ?1 and providerSubject = ?2", provider, providerSubject).firstResultOptional();
    }

    public List<AccountIdentityEntity> findByAccountId(UUID accountId) {
        return find("accountId = ?1 order by createdAt asc", accountId).list();
    }
}
