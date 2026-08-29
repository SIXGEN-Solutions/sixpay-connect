package com.sixpay.payment.application.port.input;

import com.sixpay.payment.application.command.ReceivePaymentCommand;
import com.sixpay.payment.application.view.PaymentCommandResult;

public interface PaymentReceptionUseCase {
    PaymentCommandResult receive(ReceivePaymentCommand command);
}
