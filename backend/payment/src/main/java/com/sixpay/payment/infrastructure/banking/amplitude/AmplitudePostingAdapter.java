package com.sixpay.payment.infrastructure.banking.amplitude;

import com.sixpay.payment.application.port.out.banking.PostingGateway;
import com.sixpay.payment.domain.model.evidence.PostingOutcomeSnapshot;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@ConditionalOnBean(AmplitudeBankingClient.class)
public final class AmplitudePostingAdapter
        implements PostingGateway {

    private final AmplitudeBankingClient client;

    public AmplitudePostingAdapter(AmplitudeBankingClient client) {
        this.client = Objects.requireNonNull(
                client,
                "Amplitude banking client"
        );
    }

    @Override
    public PostingOutcomeSnapshot post(
            PostingRequest request
    ) {
        return client.postPayment(request);
    }
}
