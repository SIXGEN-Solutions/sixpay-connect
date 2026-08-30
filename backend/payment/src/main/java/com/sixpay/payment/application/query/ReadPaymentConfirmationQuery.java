package com.sixpay.payment.application.query;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.payment.domain.model.PublicPaymentReference;

import java.util.Objects;

/**
 * Public read-current-confirmation application query.
 */
public record ReadPaymentConfirmationQuery(
        PublicPaymentReference paymentReference,
        CorrelationId correlationId
) {
    public ReadPaymentConfirmationQuery {
        paymentReference = Objects.requireNonNull(
                paymentReference,
                "Payment reference"
        );
        correlationId = Objects.requireNonNull(
                correlationId,
                "Correlation ID"
        );
    }
}
