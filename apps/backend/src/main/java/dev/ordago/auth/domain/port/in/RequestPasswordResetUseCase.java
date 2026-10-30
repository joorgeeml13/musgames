package dev.ordago.auth.domain.port.in;

public interface RequestPasswordResetUseCase {

    void requestPasswordReset(String email, String language);
}
