package dev.ordago.notification.application;

import java.util.Objects;
import org.jboss.logging.Logger;
import dev.ordago.notification.domain.model.EmailEvent;
import dev.ordago.notification.domain.model.EmailRecipient;
import dev.ordago.notification.domain.model.EmailSubject;
import dev.ordago.notification.domain.port.in.SendEmailUseCase;
import dev.ordago.notification.domain.port.out.EmailEventPublisherPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class EmailNotificationService implements SendEmailUseCase {

    private static final Logger LOG = Logger.getLogger(EmailNotificationService.class);

    private final EmailEventPublisherPort emailEventPublisherPort;

    @Inject
    public EmailNotificationService(EmailEventPublisherPort emailEventPublisherPort) {
        this.emailEventPublisherPort = Objects.requireNonNull(emailEventPublisherPort, "emailEventPublisherPort must not be null");
    }

    @Override
    public void sendEmail(SendEmailCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        EmailRecipient recipient = EmailRecipient.of(command.recipient());
        EmailSubject subject = EmailSubject.of(command.subject());

        EmailEvent event = EmailEvent.create(
                recipient.value(),
                recipient.value(),
                subject.value(),
                command.template(),
                command.templateData(),
                command.language()
        );

        LOG.debugf(
                "Publishing email notification event [eventId=%s, recipient=%s, template=%s]",
                event.eventId(),
                event.recipient(),
                event.template()
        );

        emailEventPublisherPort.publish(event);
    }
}
