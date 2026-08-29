package com.sixpay.payment.application.port.output.initiation;

import com.sixpay.payment.domain.model.NewPaymentIntent;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.PublicPaymentReference;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable output of initiation preparation.
 *
 * <p>It groups the generated internal/public identities with the domain intent
 * and the canonical reception time so downstream creation cannot recompute
 * any of them independently.</p>
 */
public record PreparedPaymentInitiation(
        PaymentId paymentId,
        PublicPaymentReference publicPaymentReference,
        NewPaymentIntent intent,
        Instant receivedAt
) {
    public PreparedPaymentInitiation {
        paymentId = Objects.requireNonNull(paymentId);
        publicPaymentReference = Objects.requireNonNull(publicPaymentReference);
        intent = Objects.requireNonNull(intent);
        receivedAt = Objects.requireNonNull(receivedAt);
    }
}
