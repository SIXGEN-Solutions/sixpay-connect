package com.sixpay.payment.infrastructure.banking.amplitude;

import com.sixpay.payment.application.port.out.banking.VerificationGateway;
import com.sixpay.payment.domain.model.evidence.BankingVerificationSnapshot;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@ConditionalOnBean(AmplitudeBankingClient.class)
public final class AmplitudeVerificationAdapter
        implements VerificationGateway {

    private final AmplitudeBankingClient client;

    public AmplitudeVerificationAdapter(AmplitudeBankingClient client) {
        this.client = Objects.requireNonNull(
                client,
                "Amplitude banking client"
        );
    }

    @Override
    public BankingVerificationSnapshot verify(
            VerificationRequest request
    ) {
        return client.verifyCustomerAndAccount(request);
    }
}
