package dev.ordago.auth.domain.port.out;

import java.util.Optional;
import java.util.UUID;
import dev.ordago.auth.domain.model.Account;

public interface AccountRepositoryPort {

    Account save(Account account);

    Optional<Account> findById(UUID id);

    Optional<Account> findByEmail(String email);

    boolean existsByEmail(String email);
}
