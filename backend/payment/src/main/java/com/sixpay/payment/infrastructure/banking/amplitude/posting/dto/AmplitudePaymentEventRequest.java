package com.sixpay.payment.infrastructure.banking.amplitude.posting.dto;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record AmplitudePaymentEventRequest(
        String paymentReference,
        String snapshotVersion,
        AmplitudePaymentProviderEvent providerEvent,
        List<AmplitudePaymentProviderEntry> providerEntries,
        Instant requestedAt
) {
    public AmplitudePaymentEventRequest {
        paymentReference = requireText(
                paymentReference,
                "Payment reference"
        );
        snapshotVersion = requireText(
                snapshotVersion,
                "Snapshot version"
        );
        providerEvent = Objects.requireNonNull(
                providerEvent,
                "Provider event"
        );
        providerEntries = List.copyOf(
                Objects.requireNonNull(
                        providerEntries,
                        "Provider entries"
                )
        );
        if (providerEntries.size() != 2) {
            throw new IllegalArgumentException(
                    "Amplitude Payment event requires exactly two provider entries"
            );
        }
        requestedAt = Objects.requireNonNull(
                requestedAt,
                "Requested at"
        );
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
        return value;
    }
}
