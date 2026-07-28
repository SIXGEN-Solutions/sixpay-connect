package com.sixpay.notification.infrastructure.email;

import com.sixpay.notification.application.model.PartnerDecisionNotification;
import com.sixpay.notification.infrastructure.email.smtp.SmtpEmailGateway;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class EmailPartnerNotificationSenderTest {

    @Test
    void sendsTheRenderedNotificationThroughSmtp() {
        JavaMailSender mailSender = mock(JavaMailSender.class);

        NotificationEmailProperties properties =
                new NotificationEmailProperties();
        properties.setFrom("no-reply@sixpay.example");
        properties.setSubjectPrefix("[SIXPAY TEST]");

        var sender = new EmailPartnerNotificationSender(
                new PartnerEmailTemplateRenderer(),
                new SmtpEmailGateway(mailSender),
                properties
        );

        sender.send(new PartnerDecisionNotification(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "alice.ops@example.com",
                PartnerDecisionNotification.Decision.APPROVED,
                null,
                "corr-email"
        ));

        ArgumentCaptor<SimpleMailMessage> messageCaptor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage sent = messageCaptor.getValue();

        assertThat(sent.getFrom())
                .isEqualTo("no-reply@sixpay.example");
        assertThat(sent.getTo())
                .containsExactly("alice.ops@example.com");
        assertThat(sent.getSubject())
                .startsWith("[SIXPAY TEST]");
    }
}
