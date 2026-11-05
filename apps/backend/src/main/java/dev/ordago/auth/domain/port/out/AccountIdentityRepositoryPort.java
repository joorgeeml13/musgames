package dev.ordago.auth.domain.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import dev.ordago.auth.domain.model.AccountIdentity;
import dev.ordago.auth.domain.model.OAuthProvider;

public interface AccountIdentityRepositoryPort {

    Optional<AccountIdentity> findByProviderAndSubject(OAuthProvider provider, String providerSubject);

    List<AccountIdentity> findByAccountId(UUID accountId);

    AccountIdentity save(AccountIdentity identity);
}
