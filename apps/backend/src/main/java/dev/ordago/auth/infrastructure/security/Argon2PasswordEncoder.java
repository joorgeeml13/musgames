package dev.ordago.auth.infrastructure.security;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import jakarta.enterprise.context.ApplicationScoped;
import dev.ordago.auth.domain.port.out.PasswordEncoderPort;

@ApplicationScoped
public class Argon2PasswordEncoder implements PasswordEncoderPort {

    private final int memoryKb;
    private final int iterations;
    private final int parallelism;
    private final int saltLength;
    private final int hashLength;
    private final SecureRandom secureRandom;

    public Argon2PasswordEncoder(
            @ConfigProperty(name = "ordago.auth.argon2.memory-kb", defaultValue = "65536") int memoryKb,
            @ConfigProperty(name = "ordago.auth.argon2.iterations", defaultValue = "3") int iterations,
            @ConfigProperty(name = "ordago.auth.argon2.parallelism", defaultValue = "1") int parallelism,
            @ConfigProperty(name = "ordago.auth.argon2.salt-length", defaultValue = "16") int saltLength,
            @ConfigProperty(name = "ordago.auth.argon2.hash-length", defaultValue = "32") int hashLength
    ) {
        this.memoryKb = memoryKb;
        this.iterations = iterations;
        this.parallelism = parallelism;
        this.saltLength = saltLength;
        this.hashLength = hashLength;
        this.secureRandom = new SecureRandom();
    }

    @Override
    public String encode(CharSequence rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("Password cannot be null");
        }
        byte[] salt = new byte[saltLength];
        secureRandom.nextBytes(salt);

        byte[] hash = computeArgon2id(rawPassword, salt, iterations, memoryKb, parallelism, hashLength);

        Base64.Encoder encoder = Base64.getEncoder().withoutPadding();
        return "$argon2id$v=19$m=" + memoryKb + ",t=" + iterations + ",p=" + parallelism
                + "$" + encoder.encodeToString(salt)
                + "$" + encoder.encodeToString(hash);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }

        try {
            String[] parts = encodedPassword.split("\\$");
            // parts[0] is empty, parts[1] is "argon2id", parts[2] is "v=19", parts[3] is "m=...,t=...,p=...", parts[4] is salt, parts[5] is hash
            if (parts.length != 6 || !"argon2id".equals(parts[1])) {
                return false;
            }

            int m = 65536;
            int t = 3;
            int p = 1;

            String[] params = parts[3].split(",");
            for (String param : params) {
                String[] kv = param.split("=");
                if (kv.length == 2) {
                    switch (kv[0].trim()) {
                        case "m" -> m = Integer.parseInt(kv[1].trim());
                        case "t" -> t = Integer.parseInt(kv[1].trim());
                        case "p" -> p = Integer.parseInt(kv[1].trim());
                        default -> {}
                    }
                }
            }

            Base64.Decoder decoder = Base64.getDecoder();
            byte[] salt = decoder.decode(parts[4]);
            byte[] expectedHash = decoder.decode(parts[5]);

            byte[] computedHash = computeArgon2id(rawPassword, salt, t, m, p, expectedHash.length);

            return MessageDigest.isEqual(computedHash, expectedHash);
        } catch (Exception e) {
            return false;
        }
    }

    private byte[] computeArgon2id(
            CharSequence password,
            byte[] salt,
            int iterations,
            int memoryKb,
            int parallelism,
            int outputLength
    ) {
        Argon2Parameters.Builder builder = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withVersion(Argon2Parameters.ARGON2_VERSION_13)
                .withIterations(iterations)
                .withMemoryAsKB(memoryKb)
                .withParallelism(parallelism)
                .withSalt(salt);

        Argon2BytesGenerator generator = new Argon2BytesGenerator();
        generator.init(builder.build());

        char[] chars = password.toString().toCharArray();
        try {
            byte[] result = new byte[outputLength];
            generator.generateBytes(chars, result, 0, result.length);
            return result;
        } finally {
            Arrays.fill(chars, '\0');
        }
    }
}
