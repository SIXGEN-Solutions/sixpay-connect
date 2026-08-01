package com.sixpay.payment.application.port.in;

import com.sixpay.payment.application.command.AuthorizeReversalCommand;
import com.sixpay.payment.application.command.FailPaymentWithoutFinancialEffectCommand;
import com.sixpay.payment.application.command.RecordPostingOutcomeCommand;
import com.sixpay.payment.application.command.RecordRecoverableFailureCommand;
import com.sixpay.payment.application.command.RecordReversalOutcomeCommand;
import com.sixpay.payment.application.command.RecordTfjConfirmationCommand;
import com.sixpay.payment.application.command.RejectPaymentCommand;
import com.sixpay.payment.application.command.ResolvePostingOutcomeCommand;
import com.sixpay.payment.application.command.ResolveReversalOutcomeCommand;
import com.sixpay.payment.application.view.PaymentCommandResult;

public interface PaymentFinalizationUseCase {

    PaymentCommandResult recordPostingOutcome(
            RecordPostingOutcomeCommand command
    );

    PaymentCommandResult resolvePostingOutcome(
            ResolvePostingOutcomeCommand command
    );

    PaymentCommandResult recordTfjConfirmation(
            RecordTfjConfirmationCommand command
    );

    PaymentCommandResult authorizeReversal(
            AuthorizeReversalCommand command
    );

    PaymentCommandResult recordReversalOutcome(
            RecordReversalOutcomeCommand command
    );

    PaymentCommandResult resolveReversalOutcome(
            ResolveReversalOutcomeCommand command
    );

    PaymentCommandResult reject(
            RejectPaymentCommand command
    );

    PaymentCommandResult recordRecoverableFailure(
            RecordRecoverableFailureCommand command
    );

    PaymentCommandResult failWithoutFinancialEffect(
            FailPaymentWithoutFinancialEffectCommand command
    );
}
