package com.sixpay.payment.infrastructure.banking.amplitude.error;

import com.sixpay.integration.error.ExternalFailure;
import com.sixpay.integration.error.ExternalIntegrationException;

public final class AmplitudePaymentBankingException
        extends ExternalIntegrationException {

    public AmplitudePaymentBankingException(
            ExternalFailure failure,
            Throwable cause
    ) {
        super(failure, cause);
    }
}
