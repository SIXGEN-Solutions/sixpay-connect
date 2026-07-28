package com.sixpay.notification.infrastructure.email.smtp;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

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
            String body
    ) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(requireText(from, "from"));
        message.setTo(requireText(recipient, "recipient"));
        message.setSubject(requireText(subject, "subject"));
        message.setText(requireText(body, "body"));
        mailSender.send(message);
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.strip();
    }
}
