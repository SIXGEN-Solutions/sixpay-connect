package com.sixpay.payment.application.port.input;

import com.sixpay.payment.application.command.ResendPaymentConfirmationCommand;
import com.sixpay.payment.application.view.PaymentConfirmationView;

public interface ResendPaymentConfirmationUseCase {

    PaymentConfirmationView resend(
            ResendPaymentConfirmationCommand command
    );
}
