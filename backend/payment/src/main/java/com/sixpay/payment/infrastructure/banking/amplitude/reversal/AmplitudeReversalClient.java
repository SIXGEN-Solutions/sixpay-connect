package com.sixpay.payment.infrastructure.banking.amplitude.reversal;

import com.sixpay.payment.application.port.output.banking.ReversalGateway;
import com.sixpay.payment.domain.model.evidence.ReversalSnapshot;

public interface AmplitudeReversalClient {
    ReversalSnapshot reverse(
            ReversalGateway.ReversalRequest request
    );
}
