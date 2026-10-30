package dev.ordago.auth.domain.port.in;

import java.util.UUID;

public interface ChangePasswordUseCase {

    void changePassword(UUID accountId, String currentPassword, String newPassword);
}
