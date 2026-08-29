package com.sixpay.notification.infrastructure.email;

import com.sixpay.notification.application.model.PartnerDecisionNotification;
import com.sixpay.notification.application.port.output.PartnerNotificationSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Safe local adapter: it renders the email but does not contact an SMTP
 * server and does not log the message body or rejection/suspension reason.
 */
public final class LoggingPartnerNotificationSender
        implements PartnerNotificationSender {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(LoggingPartnerNotificationSender.class);

    private final PartnerEmailTemplateRenderer renderer;
    private final NotificationEmailProperties properties;

    public LoggingPartnerNotificationSender(
            PartnerEmailTemplateRenderer renderer,
            NotificationEmailProperties properties
    ) {
        this.renderer = Objects.requireNonNull(renderer);
        this.properties = Objects.requireNonNull(properties);
    }

    @Override
    public void send(PartnerDecisionNotification notification) {
        var rendered = renderer.render(notification);
        LOGGER.info(
                "Notification email simulated: eventId={}, partnerId={}, "
                        + "recipient={}, decision={}, subject=\"{} {}\", "
                        + "correlationId={}",
                notification.eventId(),
                notification.partnerId(),
                notification.recipientEmail(),
                notification.decision(),
                properties.getSubjectPrefix(),
                rendered.subject(),
                notification.correlationId()
        );
    }
}
