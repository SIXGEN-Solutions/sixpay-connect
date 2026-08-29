package com.sixpay.payment.application.port.input;

import com.sixpay.payment.application.command.RecordTfjConfirmationCommand;
import com.sixpay.payment.application.service.PaymentWorkflowResult;

/**
 * Inbound use case dedicated exclusively to TFJ reconciliation.
 *
 * <p>Posting responses and reversal execution do not belong to this port.</p>
 */
public interface PaymentReconciliationUseCase {

    PaymentWorkflowResult reconcileTfj(
            RecordTfjConfirmationCommand command
    );
}
