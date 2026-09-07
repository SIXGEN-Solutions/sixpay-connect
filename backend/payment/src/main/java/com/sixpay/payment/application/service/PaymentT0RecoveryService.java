package com.sixpay.payment.application.service;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.common.time.TimeProvider;
import com.sixpay.payment.application.port.input.RecoverUnknownPaymentT0UseCase;
import com.sixpay.payment.application.port.output.PaymentLookupPort;
import com.sixpay.payment.application.port.output.banking.BankingRequestContext;
import com.sixpay.payment.domain.model.Payment;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.PaymentStatus;
import com.sixpay.payment.domain.model.PublicPaymentReference;
import com.sixpay.payment.domain.policy.PaymentPolicyBundle;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Durable application recovery path for uncertain T0 outcomes.
 *
 * <p>This service never invokes the financial execution POST. It delegates only
 * to PaymentEventLifecycleOrchestrationService.recover(), which performs
 * authoritative lookup through PaymentEventRecoveryPort.</p>
 */
@Service
public final class PaymentT0RecoveryService
        implements RecoverUnknownPaymentT0UseCase {

    private final PaymentLookupPort paymentLookupPort;
    private final ObjectProvider<PaymentEventLifecycleOrchestrationService>
            lifecycleProvider;
    private final PaymentPolicyBundle policies;
    private final TimeProvider timeProvider;

    public PaymentT0RecoveryService(
            PaymentLookupPort paymentLookupPort,
            ObjectProvider<PaymentEventLifecycleOrchestrationService>
                    lifecycleProvider,
            PaymentPolicyBundle policies,
            TimeProvider timeProvider
    ) {
        this.paymentLookupPort = Objects.requireNonNull(paymentLookupPort);
        this.lifecycleProvider = Objects.requireNonNull(lifecycleProvider);
        this.policies = Objects.requireNonNull(policies);
        this.timeProvider = Objects.requireNonNull(timeProvider);
    }

    @Override
    public PaymentWorkflowResult recoverByPaymentId(
            PaymentId paymentId,
            CorrelationId correlationId
    ) {
        Objects.requireNonNull(paymentId, "Payment ID");
        return recover(
                paymentLookupPort.findById(paymentId)
                        .orElseThrow(
                                () -> new PaymentNotFoundException(paymentId)
                        ),
                correlationId
        );
    }

    @Override
    public PaymentWorkflowResult recoverByPaymentReference(
            PublicPaymentReference paymentReference,
            CorrelationId correlationId
    ) {
        Objects.requireNonNull(
                paymentReference,
                "Public Payment reference"
        );

        Payment payment =
                paymentLookupPort
                        .findByPublicPaymentReference(paymentReference)
                        .orElseThrow(
                                () -> new IllegalStateException(
                                        "Payment not found for public reference "
                                                + paymentReference
                                )
                        );

        return recover(payment, correlationId);
    }

    private PaymentWorkflowResult recover(
            Payment payment,
            CorrelationId correlationId
    ) {
        Objects.requireNonNull(payment, "Payment");
        Objects.requireNonNull(correlationId, "Correlation ID");

        if (payment.status() != PaymentStatus.POSTING_OUTCOME_UNKNOWN) {
            throw new IllegalStateException(
                    "T0 recovery requires POSTING_OUTCOME_UNKNOWN Payment"
            );
        }

        PaymentEventLifecycleOrchestrationService lifecycle =
                lifecycleProvider.getIfAvailable();

        if (lifecycle == null) {
            throw new IllegalStateException(
                    "Payment T0 recovery bridge is unavailable"
            );
        }

        return lifecycle.recover(
                payment.id(),
                new BankingRequestContext(
                        correlationId,
                        payment.toState().financialInstitutionCode()
                ),
                timeProvider.now(),
                policies
        );
    }
}
