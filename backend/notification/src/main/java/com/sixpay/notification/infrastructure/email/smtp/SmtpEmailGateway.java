package com.sixpay.notification.infrastructure.email.smtp;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Thin technical gateway around Spring's SMTP client.
 */
public final class SmtpEmailGateway {

    private final JavaMailSender mailSender;

    public SmtpEmailGateway(JavaMailSender mailSender) {
        this.mailSender = Objects.requireNonNull(
                mailSender,
                "mailSender is required"
        );
    }

    public void send(
            String from,
            String recipient,
            String subject,
            String htmlBody
    ) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    StandardCharsets.UTF_8.name()
            );
            helper.setFrom(requireText(from, "from"));
            helper.setTo(requireText(recipient, "recipient"));
            helper.setSubject(requireText(subject, "subject"));
            helper.setText(requireText(htmlBody, "htmlBody"), true);
        } catch (MessagingException exception) {
            throw new MailPreparationException(
                    "Unable to prepare notification email",
                    exception
            );
        }
        mailSender.send(message);
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.strip();
    }
}
