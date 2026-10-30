package dev.ordago.notification.domain.port.in;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import dev.ordago.notification.domain.model.EmailTemplate;

public interface SendEmailUseCase {

    void sendEmail(SendEmailCommand command);

    record SendEmailCommand(
            String recipient,
            String subject,
            String template,
            Map<String, String> templateData,
            String language
    ) {
        public static final String DEFAULT_LANGUAGE = "es";

        public SendEmailCommand {
            Objects.requireNonNull(recipient, "recipient must not be null");
            Objects.requireNonNull(subject, "subject must not be null");
            Objects.requireNonNull(template, "template must not be null");
            templateData = templateData != null ? Map.copyOf(templateData) : Map.of();
            language = (language != null && !language.isBlank()) ? language.trim().toLowerCase(Locale.ROOT) : DEFAULT_LANGUAGE;
        }

        public static SendEmailCommand welcome(String recipient, String username, String language) {
            Objects.requireNonNull(username, "username must not be null");
            return new SendEmailCommand(
                    recipient,
                    "¡Bienvenido a Órdago!",
                    EmailTemplate.WELCOME.templateName(),
                    Map.of("username", username),
                    language
            );
        }

        public static SendEmailCommand emailVerification(String recipient, String verificationUrl, String language) {
            Objects.requireNonNull(verificationUrl, "verificationUrl must not be null");
            return new SendEmailCommand(
                    recipient,
                    "Verifica tu cuenta de correo",
                    EmailTemplate.EMAIL_VERIFICATION.templateName(),
                    Map.of("verificationUrl", verificationUrl),
                    language
            );
        }

        public static SendEmailCommand passwordReset(String recipient, String resetUrl, String language) {
            Objects.requireNonNull(resetUrl, "resetUrl must not be null");
            return new SendEmailCommand(
                    recipient,
                    "Restablecimiento de contraseña",
                    EmailTemplate.PASSWORD_RESET.templateName(),
                    Map.of("resetUrl", resetUrl),
                    language
            );
        }

        public static SendEmailCommand generic(String recipient, String subject, String content, String language) {
            Objects.requireNonNull(content, "content must not be null");
            return new SendEmailCommand(
                    recipient,
                    subject,
                    EmailTemplate.GENERIC.templateName(),
                    Map.of("content", content),
                    language
            );
        }
    }
}
