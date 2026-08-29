package com.sixpay.payment.application.port.input;

import com.sixpay.payment.application.command.RecordFundsControlCommand;
import com.sixpay.payment.application.view.PaymentCommandResult;

public interface PaymentFundsControlUseCase {
    PaymentCommandResult recordFundsControl(
            RecordFundsControlCommand command
    );
}
