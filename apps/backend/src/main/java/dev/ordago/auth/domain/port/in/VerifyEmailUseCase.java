package dev.ordago.auth.domain.port.in;

public interface VerifyEmailUseCase {

    void verifyEmail(String token);
}
