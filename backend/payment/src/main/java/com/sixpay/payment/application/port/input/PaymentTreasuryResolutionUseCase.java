package com.sixpay.payment.application.port.input;

import com.sixpay.payment.application.command.RecordTreasuryResolutionCommand;
import com.sixpay.payment.application.view.PaymentCommandResult;

public interface PaymentTreasuryResolutionUseCase {
    PaymentCommandResult recordResolution(
            RecordTreasuryResolutionCommand command
    );
}
