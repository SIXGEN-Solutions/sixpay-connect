package com.sixpay.payment.infrastructure.banking.amplitude;

import com.sixpay.payment.application.port.output.banking.FundsGateway;
import com.sixpay.payment.domain.model.evidence.FundsControlSnapshot;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@ConditionalOnBean(AmplitudeBankingClient.class)
public final class AmplitudeFundsAdapter
        implements FundsGateway {

    private final AmplitudeBankingClient client;

    public AmplitudeFundsAdapter(AmplitudeBankingClient client) {
        this.client = Objects.requireNonNull(
                client,
                "Amplitude banking client"
        );
    }

    @Override
    public FundsControlSnapshot check(
            FundsCheckRequest request
    ) {
        return client.checkPaymentExecution(request);
    }
}
