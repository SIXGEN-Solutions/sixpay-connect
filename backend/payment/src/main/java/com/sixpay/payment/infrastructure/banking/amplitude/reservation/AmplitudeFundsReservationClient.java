package com.sixpay.payment.infrastructure.banking.amplitude.reservation;

import com.sixpay.payment.application.port.output.banking.FundsReservationGateway;
import com.sixpay.payment.domain.model.evidence.FundsReservationSnapshot;

public interface AmplitudeFundsReservationClient {

    FundsReservationSnapshot reserve(
            FundsReservationGateway.FundsReservationRequest request
    );
}
