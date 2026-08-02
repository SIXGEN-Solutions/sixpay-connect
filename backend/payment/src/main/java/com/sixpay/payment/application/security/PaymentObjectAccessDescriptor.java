package com.sixpay.payment.application.security;

import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.PaymentSource;

import java.util.Objects;
import java.util.Optional;

/**
 * Minimal ownership metadata used for object-level authorization.
 *
 * <p>The owner is intentionally not inferred from Payment source or external
 * references.</p>
 */
public record PaymentObjectAccessDescriptor(
        PaymentId paymentId,
        PaymentSource source,
        String partnerSubject
) {
    public PaymentObjectAccessDescriptor {
        paymentId = Objects.requireNonNull(
                paymentId,
                "Payment ID"
        );
        source = Objects.requireNonNull(
                source,
                "Payment source"
        );

        if (partnerSubject != null
                && partnerSubject.isBlank()) {
            throw new IllegalArgumentException(
                    "Partner subject must be null or non-blank"
            );
        }
    }

    public Optional<String> partnerSubjectOptional() {
        return Optional.ofNullable(partnerSubject);
    }
}
