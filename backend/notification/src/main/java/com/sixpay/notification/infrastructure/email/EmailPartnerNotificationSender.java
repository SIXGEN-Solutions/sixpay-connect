package com.sixpay.notification.infrastructure.email;

import com.sixpay.notification.application.model.PartnerDecisionNotification;
import com.sixpay.notification.application.port.output.PartnerNotificationSender;
import com.sixpay.notification.infrastructure.email.smtp.SmtpEmailGateway;

import java.util.Objects;

/**
 * SMTP implementation of the Partner notification output port.
 */
public final class EmailPartnerNotificationSender
        implements PartnerNotificationSender {

    private final PartnerEmailTemplateRenderer renderer;
    private final SmtpEmailGateway gateway;
    private final NotificationEmailProperties properties;

    public EmailPartnerNotificationSender(
            PartnerEmailTemplateRenderer renderer,
            SmtpEmailGateway gateway,
            NotificationEmailProperties properties
    ) {
        this.renderer = Objects.requireNonNull(renderer);
        this.gateway = Objects.requireNonNull(gateway);
        this.properties = Objects.requireNonNull(properties);
    }

    @Override
    public void send(PartnerDecisionNotification notification) {
        var rendered = renderer.render(notification);
        gateway.send(
                properties.getFrom(),
                notification.recipientEmail(),
                prefixed(rendered.subject()),
                rendered.body()
        );
    }

    private String prefixed(String subject) {
        return properties.getSubjectPrefix() + " " + subject;
    }
}
