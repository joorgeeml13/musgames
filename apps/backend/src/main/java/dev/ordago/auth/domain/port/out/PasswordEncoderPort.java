package dev.ordago.auth.domain.port.out;

public interface PasswordEncoderPort {

    String encode(CharSequence rawPassword);

    boolean matches(CharSequence rawPassword, String encodedPassword);
}
