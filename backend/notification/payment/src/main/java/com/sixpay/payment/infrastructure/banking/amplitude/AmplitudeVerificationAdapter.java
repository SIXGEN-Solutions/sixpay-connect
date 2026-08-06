package com.sixpay.payment.infrastructure.banking.amplitude;

import com.sixpay.payment.application.port.output.banking.VerificationGateway;
import com.sixpay.payment.domain.model.evidence.BankingVerificationSnapshot;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@ConditionalOnBean(AmplitudeAccountFundsClient.class)
public final class AmplitudeVerificationAdapter
        implements VerificationGateway {

    private final AmplitudeAccountFundsClient client;

    public AmplitudeVerificationAdapter(
            AmplitudeAccountFundsClient client
    ) {
        this.client = Objects.requireNonNull(
                client,
                "Amplitude account and funds client"
        );
    }

    @Override
    public BankingVerificationSnapshot verify(
            VerificationRequest request
    ) {
        return client.verifyCustomerAndAccount(request);
    }
}
