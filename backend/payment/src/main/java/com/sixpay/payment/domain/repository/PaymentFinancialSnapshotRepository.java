package com.sixpay.payment.domain.repository;

import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.financial.PaymentFinancialEventSnapshot;

import java.util.Optional;

/**
 * Payment-private persistence boundary for finalized financial snapshots.
 */
public interface PaymentFinancialSnapshotRepository {

    PaymentFinancialEventSnapshot save(
            PaymentFinancialEventSnapshot snapshot
    );

    Optional<PaymentFinancialEventSnapshot> findByPaymentId(
            PaymentId paymentId
    );
}
