package com.sixpay.payment.application.security;

import java.util.Objects;

/**
 * Visibility applied by the projection adapter.
 */
public sealed interface PaymentVisibilityScope
        permits PaymentVisibilityScope.Internal,
                PaymentVisibilityScope.Partner {

    record Internal() implements PaymentVisibilityScope {
    }

    record Partner(String partnerSubject)
            implements PaymentVisibilityScope {

        public Partner {
            Objects.requireNonNull(
                    partnerSubject,
                    "Partner subject"
            );
            if (partnerSubject.isBlank()) {
                throw new IllegalArgumentException(
                        "Partner subject must not be blank"
                );
            }
        }
    }
}
