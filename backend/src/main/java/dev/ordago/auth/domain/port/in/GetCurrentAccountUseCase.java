package dev.ordago.auth.domain.port.in;

import java.util.UUID;
import dev.ordago.auth.domain.model.Account;

public interface GetCurrentAccountUseCase {

    Account getAccountById(UUID accountId);
}
