package com.sixpay.payment.application.port.input;

import com.sixpay.payment.application.command.InitiatePaymentCommand;
import com.sixpay.payment.application.view.PaymentInitiationResult;

/**
 * Inbound application boundary for Payment initiation.
 */
public interface PaymentInitiationUseCase {

    PaymentInitiationResult initiatePayment(
            InitiatePaymentCommand command
    );
}
