package com.sixpay.payment.infrastructure.banking.amplitude.posting.client;

import com.sixpay.payment.infrastructure.banking.amplitude.posting.dto.AmplitudePaymentEventResult;

import java.util.Optional;

public interface AmplitudePaymentEventRecoveryClient {

    Optional<AmplitudePaymentEventResult> findByPaymentReference(
            String paymentReference,
            String correlationId,
            String financialInstitutionCode
    );

    Optional<AmplitudePaymentEventResult> findByIdempotencyKey(
            String idempotencyKey,
            String correlationId,
            String financialInstitutionCode
    );
}
