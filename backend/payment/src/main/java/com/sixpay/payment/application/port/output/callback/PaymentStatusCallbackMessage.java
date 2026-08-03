package com.sixpay.payment.application.port.output.callback;

import com.sixpay.payment.domain.model.PaymentStatus;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Contracted asynchronous Payment status callback body.
 */
public record PaymentStatusCallbackMessage(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        String paymentReference,
        String endToEndId,
        String bankOperationId,
        PaymentStatus previousStatus,
        PaymentStatus status,
        String reasonCode,
        String description,
        String transactionNumber
) {

    public PaymentStatusCallbackMessage {
        eventId = Objects.requireNonNull(eventId, "Event ID");
        eventType = requireText(eventType, "Event type");
        occurredAt = Objects.requireNonNull(
                occurredAt,
                "Occurrence instant"
        );
        paymentReference = requireText(
                paymentReference,
                "Payment reference"
        );
        endToEndId = requireText(
                endToEndId,
                "End-to-end ID"
        );
        previousStatus = Objects.requireNonNull(
                previousStatus,
                "Previous Payment status"
        );
        status = Objects.requireNonNull(
                status,
                "Payment status"
        );

        if (!"PAYMENT_STATUS_CHANGED".equals(eventType)) {
            throw new IllegalArgumentException(
                    "Callback event type must be PAYMENT_STATUS_CHANGED"
            );
        }
        if (previousStatus == status) {
            throw new IllegalArgumentException(
                    "Callback must represent a real status change"
            );
        }
    }

    private static String requireText(
            String value,
            String label
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    label + " must not be blank"
            );
        }
        return value.trim();
    }
}
