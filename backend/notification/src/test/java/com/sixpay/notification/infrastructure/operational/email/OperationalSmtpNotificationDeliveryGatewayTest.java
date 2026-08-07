package com.sixpay.notification.infrastructure.operational.email;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.sixpay.notification.application.exception.PermanentNotificationDeliveryException;
import com.sixpay.notification.application.exception.RetryableNotificationDeliveryException;
import com.sixpay.notification.domain.model.NotificationDeliveryStatus;
import com.sixpay.notification.domain.policy.OperationalNotificationTemplateCatalog;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OperationalSmtpNotificationDeliveryGatewayTest {

    @Test
    void successfulSmtpSendIsAcceptedNotDelivered() {
        JavaMailSender sender =
                mock(JavaMailSender.class);

        var gateway = gateway(
                sender,
                "operations@example.test"
        );

        var result = gateway.deliver(
                OperationalEmailTemplateRendererTest
                        .paymentIntent()
        );

        assertEquals(
                NotificationDeliveryStatus.ACCEPTED,
                result.status()
        );

        ArgumentCaptor<SimpleMailMessage> captor =
                ArgumentCaptor.forClass(
                        SimpleMailMessage.class
                );

        verify(sender).send(
                captor.capture()
        );

        SimpleMailMessage message =
                captor.getValue();

        assertEquals(
                "no-reply@example.test",
                message.getFrom()
        );
        assertEquals(
                "operations@example.test",
                message.getTo()[0]
        );
        assertTrue(
                message.getSubject().contains(
                        "Paiement comptabilisé"
                )
        );
        assertTrue(
                message.getText().contains(
                        "PAY-20260807-0001"
                )
        );
    }

    @Test
    void smtpSendFailureIsRetryable() {
        JavaMailSender sender =
                mock(JavaMailSender.class);

        doThrow(
                new MailSendException(
                        "provider unavailable"
                )
        ).when(sender)
                .send(
                        any(SimpleMailMessage.class)
                );

        var gateway = gateway(
                sender,
                "operations@example.test"
        );

        RetryableNotificationDeliveryException exception =
                assertThrows(
                        RetryableNotificationDeliveryException.class,
                        () -> gateway.deliver(
                                OperationalEmailTemplateRendererTest
                                        .paymentIntent()
                        )
                );

        assertEquals(
                "SMTP_SEND_FAILED",
                exception.errorCode()
        );
    }

    @Test
    void smtpAuthenticationFailureIsPermanent() {
        JavaMailSender sender =
                mock(JavaMailSender.class);

        doThrow(
                new MailAuthenticationException(
                        "bad credentials"
                )
        ).when(sender)
                .send(
                        any(SimpleMailMessage.class)
                );

        var gateway = gateway(
                sender,
                "operations@example.test"
        );

        PermanentNotificationDeliveryException exception =
                assertThrows(
                        PermanentNotificationDeliveryException.class,
                        () -> gateway.deliver(
                                OperationalEmailTemplateRendererTest
                                        .paymentIntent()
                        )
                );

        assertEquals(
                "SMTP_AUTHENTICATION_FAILED",
                exception.errorCode()
        );
    }

    @Test
    void operationalLogsNeverExposeRawRecipientEmail() {
        JavaMailSender sender =
                mock(JavaMailSender.class);

        Logger logger =
                (Logger) LoggerFactory.getLogger(
                        OperationalSmtpNotificationDeliveryGateway.class
                );

        ListAppender<ILoggingEvent> appender =
                new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            gateway(
                    sender,
                    "operations@example.test"
            ).deliver(
                    OperationalEmailTemplateRendererTest
                            .paymentIntent()
            );
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }

        String logs =
                appender.list
                        .stream()
                        .map(
                                ILoggingEvent::getFormattedMessage
                        )
                        .reduce(
                                "",
                                (left, right) ->
                                        left + "\n" + right
                        );

        assertFalse(
                logs.contains(
                        "operations@example.test"
                )
        );

        assertTrue(
                logs.contains(
                        "o***s@example.test"
                )
        );

        assertFalse(
                logs.contains(
                        "PAY-20260807-0001"
                )
        );
    }

    private static OperationalSmtpNotificationDeliveryGateway
    gateway(
            JavaMailSender sender,
            String recipient
    ) {
        var properties =
                new OperationalNotificationEmailProperties();

        properties.setFrom(
                "no-reply@example.test"
        );
        properties.setSubjectPrefix(
                "[SIXPAY]"
        );

        return new OperationalSmtpNotificationDeliveryGateway(
                sender,
                reference -> recipient,
                new OperationalEmailTemplateRenderer(
                        new OperationalNotificationTemplateCatalog()
                ),
                properties
        );
    }
}
