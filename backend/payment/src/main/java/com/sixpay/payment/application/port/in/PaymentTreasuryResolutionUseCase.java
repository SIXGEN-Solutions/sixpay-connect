package com.sixpay.payment.application.port.in;

import com.sixpay.payment.application.command.RecordTreasuryResolutionCommand;
import com.sixpay.payment.application.view.PaymentCommandResult;

public interface PaymentTreasuryResolutionUseCase {
    PaymentCommandResult recordResolution(
            RecordTreasuryResolutionCommand command
    );
}
