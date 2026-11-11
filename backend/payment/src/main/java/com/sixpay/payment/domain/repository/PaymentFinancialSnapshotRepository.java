package com.sixpay.payment.domain.repository;

import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.financial.PaymentFinancialEventSnapshot;

import java.time.Instant;
import java.time.LocalDate;
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

    /** Enriches only bank-assigned outcomes; T0 submitted financial facts stay frozen. */
    void enrichCompletedBankingOutcome(
            PaymentId paymentId,
            String principalBankReference,
            LocalDate accountingDate,
            Instant observedAt
    );
}
