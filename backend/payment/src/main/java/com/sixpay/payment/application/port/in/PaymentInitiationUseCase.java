package com.sixpay.payment.application.port.in;

import com.sixpay.payment.application.command.InitiateDebitCommand;
import com.sixpay.payment.application.view.InitiateDebitResult;

/**
 * Inbound application boundary for TresorPay debit initiation.
 */
public interface PaymentInitiationUseCase {

    InitiateDebitResult initiateDebit(
            InitiateDebitCommand command
    );
}
