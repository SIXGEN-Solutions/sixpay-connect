package com.sixpay.notification.infrastructure.operational.email;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@ConfigurationProperties(
        prefix = OperationalNotificationEmailProperties.PREFIX
)
public class OperationalNotificationEmailProperties {

    public static final String PREFIX =
            "sixpay.notification.operational.email";

    private boolean enabled = false;
    private String from = "no-reply@sixpay.local";
    private String subjectPrefix = "[SIXPAY]";
    private List<AdminRecipient> adminRecipients =
            new ArrayList<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(
            boolean enabled
    ) {
        this.enabled = enabled;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(
            String from
    ) {
        this.from = required(
                from,
                "from"
        );
    }

    public String getSubjectPrefix() {
        return subjectPrefix;
    }

    public void setSubjectPrefix(
            String subjectPrefix
    ) {
        this.subjectPrefix = required(
                subjectPrefix,
                "subjectPrefix"
        );
    }

    public List<AdminRecipient> getAdminRecipients() {
        return List.copyOf(adminRecipients);
    }

    public void setAdminRecipients(
            List<AdminRecipient> adminRecipients
    ) {
        this.adminRecipients =
                adminRecipients == null
                        ? new ArrayList<>()
                        : new ArrayList<>(
                                adminRecipients
                        );
    }

    public record AdminRecipient(
            String reference,
            String email,
            String locale,
            boolean enabled
    ) {
        public AdminRecipient {
            reference = required(
                    reference,
                    "reference"
            );
            email = required(
                    email,
                    "email"
            );

            locale = locale == null
                    || locale.isBlank()
                    ? Locale.FRENCH.toLanguageTag()
                    : locale.strip();
        }
    }

    private static String required(
            String value,
            String name
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    name + " is required"
            );
        }

        return value.strip();
    }
}
