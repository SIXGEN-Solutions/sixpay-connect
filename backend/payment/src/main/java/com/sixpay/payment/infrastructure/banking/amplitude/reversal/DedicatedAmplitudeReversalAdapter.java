package com.sixpay.payment.infrastructure.banking.amplitude.reversal;

import com.sixpay.payment.application.port.output.banking.ReversalGateway;
import com.sixpay.payment.domain.model.evidence.ReversalSnapshot;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@ConditionalOnBean(AmplitudeReversalClient.class)
@ConditionalOnMissingBean(ReversalGateway.class)
public final class DedicatedAmplitudeReversalAdapter
        implements ReversalGateway {

    private final AmplitudeReversalClient client;

    public DedicatedAmplitudeReversalAdapter(
            AmplitudeReversalClient client
    ) {
        this.client = Objects.requireNonNull(client);
    }

    @Override
    public ReversalSnapshot reverse(ReversalRequest request) {
        return client.reverse(request);
    }
}
