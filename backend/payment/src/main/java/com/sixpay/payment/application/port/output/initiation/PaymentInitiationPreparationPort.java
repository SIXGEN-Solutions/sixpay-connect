package com.sixpay.payment.application.port.output.initiation;

import com.sixpay.payment.application.command.InitiateDebitCommand;

import java.time.Instant;

public interface PaymentInitiationPreparationPort {

    PreparedPaymentInitiation prepare(
            InitiateDebitCommand command,
            String requestHash,
            Instant receivedAt
    );
}
