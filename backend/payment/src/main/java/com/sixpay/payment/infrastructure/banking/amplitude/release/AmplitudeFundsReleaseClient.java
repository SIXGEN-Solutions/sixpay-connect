package com.sixpay.payment.infrastructure.banking.amplitude.release;

import com.sixpay.payment.application.port.output.banking.FundsReleaseGateway;
import com.sixpay.payment.domain.model.evidence.FundsReleaseSnapshot;

public interface AmplitudeFundsReleaseClient {
    FundsReleaseSnapshot release(
            FundsReleaseGateway.FundsReleaseRequest request
    );
}
