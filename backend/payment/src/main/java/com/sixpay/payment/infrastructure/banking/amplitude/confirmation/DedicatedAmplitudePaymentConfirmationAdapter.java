package com.sixpay.payment.infrastructure.banking.amplitude.confirmation;

import com.sixpay.payment.application.port.output.banking.PaymentConfirmationBankResult;
import com.sixpay.payment.application.port.output.banking.PaymentConfirmationGateway;
import com.sixpay.payment.infrastructure.banking.amplitude.confirmation.configuration.AmplitudePaymentConfirmationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@ConditionalOnProperty(
        prefix = AmplitudePaymentConfirmationProperties.PREFIX,
        name = "enabled",
        havingValue = "true"
)
@ConditionalOnBean(AmplitudePaymentConfirmationClient.class)
@ConditionalOnMissingBean(PaymentConfirmationGateway.class)
public final class DedicatedAmplitudePaymentConfirmationAdapter implements PaymentConfirmationGateway {
    private final AmplitudePaymentConfirmationClient client;

    public DedicatedAmplitudePaymentConfirmationAdapter(AmplitudePaymentConfirmationClient client) {
        this.client = Objects.requireNonNull(client, "Amplitude Payment Confirmation client");
    }

    @Override public PaymentConfirmationBankResult create(CreateRequest request) { return client.create(request); }
    @Override public PaymentConfirmationBankResult verify(VerifyRequest request) { return client.verify(request); }
    @Override public PaymentConfirmationBankResult replace(ReplaceRequest request) { return client.replace(request); }
    @Override public PaymentConfirmationBankResult lookup(LookupRequest request) { return client.lookup(request); }
    @Override public PaymentConfirmationBankResult recover(RecoveryRequest request) { return client.recover(request); }
    @Override public PaymentConfirmationBankResult revoke(RevokeRequest request) { return client.revoke(request); }
}
