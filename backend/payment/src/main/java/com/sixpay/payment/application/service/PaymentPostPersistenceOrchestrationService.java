package com.sixpay.payment.application.service;

import com.sixpay.common.messaging.model.IntegrationEventEnvelope;
import com.sixpay.common.time.TimeProvider;
import com.sixpay.payment.application.port.input.HandlePaymentPostPersistenceEventUseCase;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.PaymentStatus;
import com.sixpay.payment.domain.policy.PaymentPolicyBundle;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Objects;

/**
 * Orchestrates Payment workflow steps strictly after durable persistence.
 */
@Service
public final class PaymentPostPersistenceOrchestrationService
        implements HandlePaymentPostPersistenceEventUseCase {

    static final String PAYMENT_AGGREGATE_TYPE = "PAYMENT";
    static final String PAYMENT_RECEIVED_EVENT_TYPE = "PaymentReceived";
    static final String BANKING_VERIFICATION_REQUESTED_EVENT_TYPE =
            "PaymentBankingVerificationRequested";

    private final PaymentMutationCoordinator coordinator;
    private final ObjectProvider<PaymentCustomerVerificationService>
            customerVerificationServiceProvider;
    private final PaymentPolicyBundle policies;
    private final TimeProvider timeProvider;

    public PaymentPostPersistenceOrchestrationService(
            PaymentMutationCoordinator coordinator,
            ObjectProvider<PaymentCustomerVerificationService>
                    customerVerificationServiceProvider,
            PaymentPolicyBundle policies,
            TimeProvider timeProvider
    ) {
        this.coordinator = Objects.requireNonNull(
                coordinator,
                "Payment mutation coordinator is required"
        );
        this.customerVerificationServiceProvider = Objects.requireNonNull(
                customerVerificationServiceProvider,
                "Customer verification service provider is required"
        );
        this.policies = Objects.requireNonNull(
                policies,
                "Payment policy bundle is required"
        );
        this.timeProvider = Objects.requireNonNull(
                timeProvider,
                "Time provider is required"
        );
    }

    @Override
    public void handle(IntegrationEventEnvelope event) {
        Objects.requireNonNull(event, "Integration event is required");

        if (!PAYMENT_AGGREGATE_TYPE.equals(event.aggregateType())) {
            return;
        }

        PaymentId paymentId = new PaymentId(event.aggregateId());

        if (PAYMENT_RECEIVED_EVENT_TYPE.equals(event.eventType())) {
            startBankingVerification(paymentId);
            return;
        }

        if (BANKING_VERIFICATION_REQUESTED_EVENT_TYPE.equals(
                event.eventType()
        )) {
            verifyCustomer(paymentId);
        }
    }

    private void startBankingVerification(PaymentId paymentId) {
        Instant requestedAt = timeProvider.now();

        coordinator.mutate(
                paymentId,
                payment -> {
                    if (payment.status() == PaymentStatus.RECEIVED
                            || payment.status()
                            == PaymentStatus.BANKING_VERIFICATION_PENDING) {
                        payment.startBankingVerification(requestedAt);
                    }
                }
        );
    }

    private void verifyCustomer(PaymentId paymentId) {
        PaymentCustomerVerificationService customerVerificationService =
                customerVerificationServiceProvider.getIfAvailable();

        if (customerVerificationService == null) {
            throw new IllegalStateException(
                    "Customer Verification bridge is unavailable"
            );
        }

        customerVerificationService.verifyCustomer(
                paymentId,
                timeProvider.now(),
                policies
        );
    }
}
