package com.sixpay.notification.infrastructure.email;

import com.sixpay.notification.application.model.PartnerDecisionNotification;
import com.sixpay.notification.infrastructure.email.smtp.SmtpEmailGateway;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailPartnerNotificationSenderTest {

    @Test
    void sendsTheRenderedHtmlNotificationThroughSmtp() throws Exception {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        MimeMessage mimeMessage = new MimeMessage(
                Session.getInstance(new Properties())
        );
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

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

        verify(mailSender).send(mimeMessage);
        mimeMessage.saveChanges();

        assertThat(mimeMessage.getFrom()[0].toString())
                .isEqualTo("no-reply@sixpay.example");
        assertThat(mimeMessage.getAllRecipients()[0].toString())
                .isEqualTo("alice.ops@example.com");
        assertThat(mimeMessage.getSubject())
                .startsWith("[SIXPAY TEST]");
        assertThat(mimeMessage.getContentType())
                .containsIgnoringCase("text/html");
        assertThat(mimeMessage.getContent().toString())
                .contains("<!doctype html>")
                .contains("Votre accès est actif");
    }
}
