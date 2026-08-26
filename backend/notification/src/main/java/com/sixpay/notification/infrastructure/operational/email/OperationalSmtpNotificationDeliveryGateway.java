package com.sixpay.notification.infrastructure.operational.email;

import com.sixpay.notification.application.exception.PermanentNotificationDeliveryException;
import com.sixpay.notification.application.exception.RetryableNotificationDeliveryException;
import com.sixpay.notification.application.port.output.AdminEmailAddressResolver;
import com.sixpay.notification.application.port.output.NotificationDispatchResult;
import com.sixpay.notification.application.port.output.OperationalNotificationDeliveryGateway;
import com.sixpay.notification.domain.model.NotificationChannel;
import com.sixpay.notification.domain.model.NotificationIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailParseException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Objects;

public final class OperationalSmtpNotificationDeliveryGateway
        implements OperationalNotificationDeliveryGateway {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(
                    OperationalSmtpNotificationDeliveryGateway.class
            );

    private final JavaMailSender mailSender;
    private final AdminEmailAddressResolver addressResolver;
    private final OperationalEmailTemplateRenderer renderer;
    private final OperationalNotificationEmailProperties properties;

    public OperationalSmtpNotificationDeliveryGateway(
            JavaMailSender mailSender,
            AdminEmailAddressResolver addressResolver,
            OperationalEmailTemplateRenderer renderer,
            OperationalNotificationEmailProperties properties
    ) {
        this.mailSender = Objects.requireNonNull(
                mailSender,
                "mailSender"
        );
        this.addressResolver = Objects.requireNonNull(
                addressResolver,
                "addressResolver"
        );
        this.renderer = Objects.requireNonNull(
                renderer,
                "renderer"
        );
        this.properties = Objects.requireNonNull(
                properties,
                "properties"
        );
    }

    @Override
    public NotificationDispatchResult deliver(
            NotificationIntent notification
    ) {
        Objects.requireNonNull(
                notification,
                "notification"
        );

        if (notification.channel()
                != NotificationChannel.EMAIL) {
            throw new PermanentNotificationDeliveryException(
                    "UNSUPPORTED_NOTIFICATION_CHANNEL",
                    "Operational SMTP gateway supports EMAIL only",
                    null
            );
        }

        String recipient;

        try {
            recipient = addressResolver.resolveEmail(
                    notification.recipient()
                            .reference()
            );
        } catch (RuntimeException exception) {
            throw new PermanentNotificationDeliveryException(
                    "ADMIN_RECIPIENT_NOT_RESOLVED",
                    "SIXPAY admin recipient cannot be resolved",
                    exception
            );
        }

        OperationalEmailTemplateRenderer.RenderedOperationalEmail
                rendered;

        try {
            rendered = renderer.render(
                    notification,
                    properties.getSubjectPrefix()
            );
        } catch (RuntimeException exception) {
            throw new PermanentNotificationDeliveryException(
                    "EMAIL_TEMPLATE_INVALID",
                    "Operational notification template cannot be rendered",
                    exception
            );
        }

        SimpleMailMessage message =
                new SimpleMailMessage();

        message.setFrom(
                properties.getFrom()
        );
        message.setTo(recipient);
        message.setSubject(
                rendered.subject()
        );
        message.setText(
                rendered.body()
        );

        String maskedRecipient =
                NotificationEmailMasker.mask(
                        recipient
                );

        try {
            mailSender.send(message);

            LOGGER.info(
                    "Operational notification accepted by SMTP: "
                            + "notificationId={}, template={}, recipient={}, "
                            + "correlationId={}",
                    notification.notificationId(),
                    notification.templateKey(),
                    maskedRecipient,
                    notification.correlationId()
            );

            /*
             * SMTP send proves provider/server acceptance only.
             * It does not prove delivery to the administrator mailbox.
             */
            return NotificationDispatchResult.accepted(
                    null
            );
        } catch (MailAuthenticationException exception) {
            throw permanent(
                    "SMTP_AUTHENTICATION_FAILED",
                    notification,
                    maskedRecipient,
                    exception
            );
        } catch (MailParseException
                 | MailPreparationException exception) {
            throw permanent(
                    "SMTP_MESSAGE_INVALID",
                    notification,
                    maskedRecipient,
                    exception
            );
        } catch (MailSendException exception) {
            throw retryable(
                    "SMTP_SEND_FAILED",
                    notification,
                    maskedRecipient,
                    exception
            );
        } catch (MailException exception) {
            throw retryable(
                    "SMTP_TEMPORARILY_UNAVAILABLE",
                    notification,
                    maskedRecipient,
                    exception
            );
        }
    }

    private static PermanentNotificationDeliveryException permanent(
            String errorCode,
            NotificationIntent notification,
            String maskedRecipient,
            RuntimeException cause
    ) {
        LOGGER.warn(
                "Permanent operational email failure: "
                        + "notificationId={}, template={}, recipient={}, "
                        + "correlationId={}, errorCode={}, causeType={}",
                notification.notificationId(),
                notification.templateKey(),
                maskedRecipient,
                notification.correlationId(),
                errorCode,
                cause.getClass().getSimpleName()
        );

        return new PermanentNotificationDeliveryException(
                errorCode,
                "Operational email delivery failed permanently",
                cause
        );
    }

    private static RetryableNotificationDeliveryException retryable(
            String errorCode,
            NotificationIntent notification,
            String maskedRecipient,
            RuntimeException cause
    ) {
        LOGGER.warn(
                "Retryable operational email failure: "
                        + "notificationId={}, template={}, recipient={}, "
                        + "correlationId={}, errorCode={}, causeType={}",
                notification.notificationId(),
                notification.templateKey(),
                maskedRecipient,
                notification.correlationId(),
                errorCode,
                cause.getClass().getSimpleName()
        );

        return new RetryableNotificationDeliveryException(
                errorCode,
                "Operational email delivery failed temporarily",
                cause
        );
    }
}
