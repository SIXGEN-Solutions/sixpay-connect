package com.sixpay.payment.infrastructure.banking.amplitude.posting.client;

import com.sixpay.payment.infrastructure.banking.amplitude.posting.dto.AmplitudePaymentEventRequest;
import com.sixpay.payment.infrastructure.banking.amplitude.posting.dto.AmplitudePaymentEventResult;

public interface AmplitudePaymentEventClient {

    AmplitudePaymentEventResult execute(
            AmplitudePaymentEventRequest request,
            String correlationId,
            String financialInstitutionCode,
            String idempotencyKey
    );
}
