package com.sixpay.payment.infrastructure.banking.amplitude.reservation;

import com.sixpay.payment.application.port.output.banking.FundsReservationGateway;
import com.sixpay.payment.domain.model.evidence.FundsReservationSnapshot;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@ConditionalOnBean(AmplitudeFundsReservationClient.class)
public final class AmplitudeFundsReservationAdapter
        implements FundsReservationGateway {

    private final AmplitudeFundsReservationClient client;

    public AmplitudeFundsReservationAdapter(
            AmplitudeFundsReservationClient client
    ) {
        this.client = Objects.requireNonNull(
                client,
                "Amplitude funds reservation client"
        );
    }

    @Override
    public FundsReservationSnapshot reserve(
            FundsReservationRequest request
    ) {
        return client.reserve(request);
    }
}
