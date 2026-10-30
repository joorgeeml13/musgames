package dev.ordago.auth.domain.port.in;

public interface ResendVerificationEmailUseCase {

    void resendVerificationEmail(String email, String language);
}
