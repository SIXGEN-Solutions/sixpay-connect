package com.sixpay.payment.application.service;

import com.sixpay.payment.application.command.RecordTfjConfirmationCommand;
import com.sixpay.payment.application.port.in.PaymentReconciliationUseCase;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Applies one uniquely matched TFJ confirmation to an already posted Payment.
 *
 * <p>This service never submits or looks up a bank posting. It consumes
 * authoritative end-of-day evidence and delegates the reconciliation decision
 * to the Payment Aggregate Root.</p>
 */
@Service
public class PaymentReconciliationService
        implements PaymentReconciliationUseCase {

    private final PaymentMutationCoordinator coordinator;

    public PaymentReconciliationService(
            PaymentMutationCoordinator coordinator
    ) {
        this.coordinator = Objects.requireNonNull(
                coordinator,
                "Payment mutation coordinator"
        );
    }

    @Override
    public PaymentWorkflowResult reconcileTfj(
            RecordTfjConfirmationCommand command
    ) {
        Objects.requireNonNull(
                command,
                "TFJ reconciliation command"
        );

        return coordinator.mutate(
                command.paymentId(),
                payment ->
                        payment.recordMatchedEndOfDayConfirmation(
                                command.evidence(),
                                command.matchProof(),
                                command.reconciliationFailure(),
                                command.decisionAt(),
                                command.policies()
                        )
        );
    }
}
