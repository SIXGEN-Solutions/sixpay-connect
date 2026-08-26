package com.sixpay.payment.application.port.output;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.payment.domain.model.evidence.BankingVerificationSnapshot;

import java.time.Instant;

/**
 * Payment-owned mapping boundary from an intermodule response to canonical
 * Payment banking evidence.
 */
public interface CustomerVerificationEvidenceMapper {

    BankingVerificationSnapshot toSnapshot(
            CustomerVerificationResponse response,
            CorrelationId correlationId,
            Instant acceptedAt
    );
}
