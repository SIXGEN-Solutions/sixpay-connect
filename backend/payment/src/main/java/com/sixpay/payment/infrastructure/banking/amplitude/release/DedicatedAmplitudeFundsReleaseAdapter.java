package com.sixpay.payment.infrastructure.banking.amplitude.release;

import com.sixpay.payment.application.port.output.banking.FundsReleaseGateway;
import com.sixpay.payment.domain.model.evidence.FundsReleaseSnapshot;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@ConditionalOnBean(AmplitudeFundsReleaseClient.class)
@ConditionalOnMissingBean(FundsReleaseGateway.class)
public final class DedicatedAmplitudeFundsReleaseAdapter
        implements FundsReleaseGateway {

    private final AmplitudeFundsReleaseClient client;

    public DedicatedAmplitudeFundsReleaseAdapter(
            AmplitudeFundsReleaseClient client
    ) {
        this.client = Objects.requireNonNull(client);
    }

    @Override
    public FundsReleaseSnapshot release(
            FundsReleaseRequest request
    ) {
        return client.release(request);
    }
}
