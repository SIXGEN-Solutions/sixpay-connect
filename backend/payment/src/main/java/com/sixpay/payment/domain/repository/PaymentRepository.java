package com.sixpay.payment.domain.repository;

import com.sixpay.payment.domain.model.ExternalPaymentReference;
import com.sixpay.payment.domain.model.Payment;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.PaymentSource;
import com.sixpay.payment.domain.model.PublicPaymentReference;

import java.util.Optional;

/**
 * Persistence boundary owned by the Payment domain.
 *
 * <p>The interface is framework-free. Infrastructure implementations must
 * reconstitute complete {@link Payment} aggregates and must never expose JPA
 * entities outside the persistence adapter.</p>
 */
public interface PaymentRepository {

    Payment save(Payment payment);

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
