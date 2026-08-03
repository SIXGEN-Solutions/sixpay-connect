package com.sixpay.payment.infrastructure.banking.amplitude;

import com.sixpay.payment.application.port.output.banking.ReversalGateway;
import com.sixpay.payment.domain.model.evidence.ReversalSnapshot;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@ConditionalOnBean(AmplitudeBankingClient.class)
public final class AmplitudeReversalAdapter
        implements ReversalGateway {

    private final AmplitudeBankingClient client;

    public AmplitudeReversalAdapter(AmplitudeBankingClient client) {
        this.client = Objects.requireNonNull(
                client,
                "Amplitude banking client"
        );
    }

    @Override
    public ReversalSnapshot reverse(
            ReversalRequest request
    ) {
        return client.reversePayment(request);
    }
}
