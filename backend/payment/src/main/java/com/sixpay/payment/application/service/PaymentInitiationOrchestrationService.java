package com.sixpay.payment.application.service;

import com.sixpay.common.time.TimeProvider;
import com.sixpay.payment.application.command.InitiateDebitCommand;
import com.sixpay.payment.application.port.in.PaymentInitiationUseCase;
import com.sixpay.payment.application.port.out.idempotency
        .PaymentInitiationIdempotencyPort;
import com.sixpay.payment.application.port.out.initiation
        .PaymentInitiationPreparationPort;
import com.sixpay.payment.application.port.out.initiation
        .PreparedPaymentInitiation;
import com.sixpay.payment.application.view.InitiateDebitResult;
import com.sixpay.payment.domain.model.PaymentStatus;
import com.sixpay.sharedkernel.domain.valueobject.Money;
import org.springframework.boot.autoconfigure.condition
        .ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Idempotent application orchestration for TresorPay InitiateDebit.
 *
 * <p>The orchestration stops after SIXPAY atomically persists the Payment
 * in {@link PaymentStatus#PENDING_CONFIRMATION}. It does not contact the
 * core banking and does not execute the TresorPay callback.</p>
 */
@Service
@ConditionalOnBean({
        PaymentInitiationIdempotencyPort.class,
        PaymentInitiationPreparationPort.class
})
public class PaymentInitiationOrchestrationService
        implements PaymentInitiationUseCase {

    private final PaymentInitiationIdempotencyPort idempotencyPort;
    private final PaymentInitiationPreparationPort preparationPort;
    private final PaymentReceptionService receptionService;
    private final TimeProvider timeProvider;

    public PaymentInitiationOrchestrationService(
            PaymentInitiationIdempotencyPort idempotencyPort,
            PaymentInitiationPreparationPort preparationPort,
            PaymentReceptionService receptionService,
            TimeProvider timeProvider
    ) {
        this.idempotencyPort = Objects.requireNonNull(
                idempotencyPort,
                "Payment initiation idempotency port"
        );

        this.preparationPort = Objects.requireNonNull(
                preparationPort,
                "Payment initiation preparation port"
        );

        this.receptionService = Objects.requireNonNull(
                receptionService,
                "Payment reception service"
        );

        this.timeProvider = Objects.requireNonNull(
                timeProvider,
                "Time provider"
        );
    }

    @Override
    public InitiateDebitResult initiateDebit(
            InitiateDebitCommand command
    ) {
        Objects.requireNonNull(
                command,
                "InitiateDebit command"
        );

        return idempotencyPort.execute(
                command,
                requestHash -> initiateNew(
                        command,
                        requestHash
                )
        );
    }

    private InitiateDebitResult initiateNew(
            InitiateDebitCommand command,
            String requestHash
    ) {
        var receivedAt = timeProvider.now();

        PreparedPaymentInitiation prepared =
                preparationPort.prepare(
                        command,
                        requestHash,
                        receivedAt
                );

        PaymentWorkflowResult workflow =
                receptionService.receive(
                        prepared.paymentId(),
                        prepared.publicPaymentReference(),
                        prepared.intent(),
                        prepared.receivedAt()
                );

        if (workflow.status()
                != PaymentStatus.PENDING_CONFIRMATION) {

            throw new IllegalStateException(
                    "InitiateDebit must persist Payment in "
                            + "PENDING_CONFIRMATION"
            );
        }

        Money totalAmount = Money.of(
                command.totalAmount(),
                command.currency()
        );

        return InitiateDebitResult.accepted(
                workflow.paymentId(),
                workflow.publicPaymentReference(),
                command.endToEndId(),
                totalAmount,
                prepared.receivedAt()
        );
    }
}