package com.sixpay.payment.application.port.input;

import com.sixpay.payment.application.command.RecordSixpayAuthorizationDecisionCommand;
import com.sixpay.payment.application.command.RecordBankingVerificationCommand;
import com.sixpay.payment.application.command.StartAuthorizationCommand;
import com.sixpay.payment.application.view.PaymentCommandResult;

public interface PaymentAuthorizationUseCase {

    PaymentCommandResult startAuthorization(
            StartAuthorizationCommand command
    );

    PaymentCommandResult recordAuthorizationDecision(
            RecordSixpayAuthorizationDecisionCommand command
    );

    PaymentCommandResult recordBankingVerification(
            RecordBankingVerificationCommand command
    );
}
