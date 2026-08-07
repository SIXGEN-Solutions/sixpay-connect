package com.sixpay.notification.infrastructure.operational.email;

public final class NotificationEmailMasker {

    private NotificationEmailMasker() {
    }

    public static String mask(
            String email
    ) {
        if (email == null || email.isBlank()) {
            return "***";
        }

        String normalized = email.strip();
        int separator = normalized.indexOf('@');

        if (separator <= 0
                || separator == normalized.length() - 1) {
            return "***";
        }

        String local = normalized.substring(
                0,
                separator
        );

        String domain = normalized.substring(
                separator + 1
        );

        String maskedLocal =
                local.length() == 1
                        ? local.charAt(0) + "***"
                        : local.charAt(0)
                        + "***"
                        + local.charAt(
                                local.length() - 1
                        );

        return maskedLocal
                + "@"
                + domain;
    }
}
