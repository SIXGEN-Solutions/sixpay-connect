package com.sixpay.payment.application.port.in;

import com.sixpay.payment.application.command.AuthorizePostingCommand;
import com.sixpay.payment.application.view.PaymentCommandResult;

public interface PaymentPostingPreparationUseCase {
    PaymentCommandResult authorizePosting(
            AuthorizePostingCommand command
    );
}
