package com.sixpay.payment.application.port.input;

import com.sixpay.payment.application.command.CreatePaymentConfirmationCommand;
import com.sixpay.payment.application.view.PaymentConfirmationView;

public interface CreatePaymentConfirmationUseCase {

    PaymentConfirmationView create(
            CreatePaymentConfirmationCommand command
    );
}
