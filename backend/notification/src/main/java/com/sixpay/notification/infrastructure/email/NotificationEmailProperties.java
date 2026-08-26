package com.sixpay.notification.infrastructure.email;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration of the notification email adapter.
 */
@ConfigurationProperties(prefix = "sixpay.notification.email")
public class NotificationEmailProperties {

    private Mode mode = Mode.LOGGING;
    private String from = "no-reply@sixpay.local";
    private String subjectPrefix = "[SIXPAY]";

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode == null ? Mode.LOGGING : mode;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = requireText(from, "from");
    }

    public String getSubjectPrefix() {
        return subjectPrefix;
    }

    public void setSubjectPrefix(String subjectPrefix) {
        this.subjectPrefix = requireText(subjectPrefix, "subjectPrefix");
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.strip();
    }

    public enum Mode {
        LOGGING,
        SMTP
    }
}
