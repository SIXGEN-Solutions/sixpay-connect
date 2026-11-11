package com.sixpay.payment.infrastructure.banking.amplitude.posting.dto;

import java.time.Instant;
import java.util.Objects;

/** Regional PaymentEventEnvelope: exact four contractual fields. */
public class AmplitudePaymentEventRequest {
    private String paymentReference;
    private String snapshotVersion;
    private AmplitudePaymentProviderEvent providerEvent;
    private Instant requestedAt;

    public AmplitudePaymentEventRequest() { }

    public AmplitudePaymentEventRequest(String paymentReference, String snapshotVersion,
            AmplitudePaymentProviderEvent providerEvent, Instant requestedAt) {
        setPaymentReference(paymentReference);
        setSnapshotVersion(snapshotVersion);
        setProviderEvent(providerEvent);
        setRequestedAt(requestedAt);
    }

    public String paymentReference() { return paymentReference; }
    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String value) { paymentReference = requireText(value, "paymentReference"); }

    public String snapshotVersion() { return snapshotVersion; }
    public String getSnapshotVersion() { return snapshotVersion; }
    public void setSnapshotVersion(String value) { snapshotVersion = requireText(value, "snapshotVersion"); }

    public AmplitudePaymentProviderEvent providerEvent() { return providerEvent; }
    public AmplitudePaymentProviderEvent getProviderEvent() { return providerEvent; }
    public void setProviderEvent(AmplitudePaymentProviderEvent value) {
        providerEvent = Objects.requireNonNull(value, "providerEvent");
    }

    public Instant requestedAt() { return requestedAt; }
    public Instant getRequestedAt() { return requestedAt; }
    public void setRequestedAt(Instant value) { requestedAt = Objects.requireNonNull(value, "requestedAt"); }

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " is required");
        return value.strip();
    }
}
