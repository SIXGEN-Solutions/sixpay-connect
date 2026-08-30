package com.sixpay.payment.application.port.input;

import com.sixpay.payment.application.command.VerifyPaymentConfirmationCommand;
import com.sixpay.payment.application.view.PaymentConfirmationView;

public interface VerifyPaymentConfirmationUseCase {

    PaymentConfirmationView verify(
            VerifyPaymentConfirmationCommand command
    );
}
