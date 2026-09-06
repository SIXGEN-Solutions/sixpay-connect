package com.sixpay.payment.infrastructure.banking.amplitude.posting;

import com.sixpay.payment.application.port.output.banking.PaymentEventExecutionPort;
import com.sixpay.payment.infrastructure.banking.amplitude.posting.client.AmplitudePaymentEventClient;
import com.sixpay.payment.infrastructure.banking.amplitude.posting.dto.AmplitudePaymentEventResult;

import java.util.Objects;

public final class AmplitudePaymentEventAdapter
        implements PaymentEventExecutionPort {

    private final AmplitudePaymentEventClient client;

    public AmplitudePaymentEventAdapter(
            AmplitudePaymentEventClient client
    ) {
        this.client = Objects.requireNonNull(
                client,
                "Amplitude Payment event client"
        );
    }

    @Override
    public AmplitudePaymentEventResult execute(
            PaymentEventExecutionCommand command
    ) {
        Objects.requireNonNull(
                command,
                "Payment event execution command"
        );

        return client.execute(
                command.request(),
                command.context()
                        .correlationId()
                        .value(),
                command.context()
                        .financialInstitutionCode()
                        .value(),
                command.idempotencyKey()
                        .value()
        );
    }
}
