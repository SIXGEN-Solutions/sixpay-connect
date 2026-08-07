package com.sixpay.notification.application.port.input;

public record OperationalNotificationReplayCommand(
        String operatorReference,
        String reason
) {
    public OperationalNotificationReplayCommand {
        operatorReference = required(
                operatorReference,
                "operatorReference",
                128
        );
        reason = required(
                reason,
                "reason",
                500
        );
    }

    private static String required(
            String value,
            String name,
            int maxLength
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    name + " is required"
            );
        }

        String normalized = value.strip();

        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(
                    name + " exceeds " + maxLength + " characters"
            );
        }

        return normalized;
    }
}
