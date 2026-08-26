package com.sixpay.payment.infrastructure.banking.amplitude.posting;

import com.sixpay.payment.application.port.output.banking.PostingGateway;
import com.sixpay.payment.domain.model.evidence.PostingOutcomeSnapshot;

public interface AmplitudePostingClient {

    PostingOutcomeSnapshot post(
            PostingGateway.PostingRequest request
    );
}
