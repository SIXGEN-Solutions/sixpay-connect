package com.sixpay.payment.application.service;

import com.sixpay.common.time.TimeProvider;
import com.sixpay.payment.application.command.InitiateDebitCommand;
import com.sixpay.payment.application.port.in.PaymentInitiationUseCase;
import com.sixpay.payment.application.port.output.idempotency.PaymentInitiationIdempotencyPort;
import com.sixpay.payment.application.port.output.initiation.PaymentInitiationPreparationPort;
import com.sixpay.payment.application.port.output.initiation.PreparedPaymentInitiation;
import com.sixpay.payment.application.view.InitiateDebitResult;
import com.sixpay.payment.domain.model.PaymentStatus;
import com.sixpay.sharedkernel.domain.valueobject.Money;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Objects;

/**
 * Coordinates the synchronous portion of {@code POST /v1/payments/initiate}.
 *
 * <p>The service first enters the idempotency boundary. Only a new request is
 * prepared and received by the Payment aggregate; a completed request is
 * returned from the durable replay store. A successful new call finishes in
 * {@link PaymentStatus#PENDING_CONFIRMATION}; later authorization, banking and
 * posting stages are handled by separate workflow services.</p>
 */
@Service
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
        this.idempotencyPort = Objects.requireNonNull(idempotencyPort);
        this.preparationPort = Objects.requireNonNull(preparationPort);
        this.receptionService = Objects.requireNonNull(receptionService);
        this.timeProvider = Objects.requireNonNull(timeProvider);
    }

    /**
     * Initiates or replays a debit request under its idempotency key.
     *
     * @param command validated command containing partner, business and
     *                request-control data
     * @return the stable accepted result for this idempotency key
     */
    @Override
    public InitiateDebitResult initiateDebit(
            InitiateDebitCommand command
    ) {
        Objects.requireNonNull(command);

        return idempotencyPort.execute(
                command,
                requestHash -> initiateNew(command, requestHash)
        );
    }

    /**
     * Performs the new-request path. The supplied hash is the exact
     * fingerprint already registered by the idempotency adapter and becomes
     * part of the Payment request identity.
     */
    private InitiateDebitResult initiateNew(
            InitiateDebitCommand command,
            String requestHash
    ) {
        Instant receivedAt = timeProvider.now();

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
                    "InitiateDebit must persist Payment in PENDING_CONFIRMATION"
            );
        }

        return InitiateDebitResult.accepted(
                workflow.paymentId(),
                workflow.publicPaymentReference(),
                command.endToEndId(),
                Money.of(command.totalAmount(), command.currency()),
                prepared.receivedAt()
        );
    }
}
