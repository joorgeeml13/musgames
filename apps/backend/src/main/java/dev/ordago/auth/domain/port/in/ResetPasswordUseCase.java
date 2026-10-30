package dev.ordago.auth.domain.port.in;

public interface ResetPasswordUseCase {

    void resetPassword(String token, String newPassword);
}
