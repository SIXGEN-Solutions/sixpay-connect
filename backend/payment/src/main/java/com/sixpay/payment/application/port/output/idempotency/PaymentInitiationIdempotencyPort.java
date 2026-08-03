package com.sixpay.payment.application.port.output.idempotency;

import com.sixpay.payment.application.command.InitiateDebitCommand;
import com.sixpay.payment.application.view.InitiateDebitResult;

import java.util.function.Function;

public interface PaymentInitiationIdempotencyPort {

    InitiateDebitResult execute(
            InitiateDebitCommand command,
            Function<String, InitiateDebitResult> newRequest
    );
}
