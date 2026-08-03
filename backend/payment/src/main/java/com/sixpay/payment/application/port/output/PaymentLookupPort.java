package com.sixpay.payment.application.port.output;

import com.sixpay.payment.domain.model.ExternalPaymentReference;
import com.sixpay.payment.domain.model.Payment;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.PaymentSource;
import com.sixpay.payment.domain.model.PublicPaymentReference;

import java.util.Optional;

/**
 * Application lookup boundary for complete Payment aggregates.
 */
public interface PaymentLookupPort {

    Optional<Payment> findById(PaymentId paymentId);

    Optional<Payment> findByPublicPaymentReference(
            PublicPaymentReference publicPaymentReference
    );

    Optional<Payment> findBySourceAndExternalPaymentReference(
            PaymentSource source,
            ExternalPaymentReference externalPaymentReference
    );

    boolean existsBySourceAndExternalPaymentReference(
            PaymentSource source,
            ExternalPaymentReference externalPaymentReference
    );
}
