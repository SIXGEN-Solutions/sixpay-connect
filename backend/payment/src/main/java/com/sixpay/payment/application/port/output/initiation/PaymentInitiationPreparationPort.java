package com.sixpay.payment.application.port.output.initiation;

import com.sixpay.payment.application.command.InitiateDebitCommand;

import java.time.Instant;

/**
 * Converts a validated API command into Payment domain identities and a new
 * intent without persisting or advancing the aggregate.
 */
public interface PaymentInitiationPreparationPort {

    /**
     * Prepares one new Payment using the fingerprint computed by the
     * idempotency boundary and the single reception timestamp selected by the
     * orchestration service.
     */
    PreparedPaymentInitiation prepare(
            InitiateDebitCommand command,
            String requestHash,
            Instant receivedAt
    );
}
