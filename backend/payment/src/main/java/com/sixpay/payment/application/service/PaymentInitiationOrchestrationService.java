package com.sixpay.payment.application.service;

import com.sixpay.common.time.TimeProvider;
import com.sixpay.payment.application.command.CreatePaymentConfirmationCommand;
import com.sixpay.payment.application.command.InitiatePaymentCommand;
import com.sixpay.payment.application.port.input.PaymentInitiationUseCase;
import com.sixpay.payment.application.port.output.idempotency.PaymentInitiationIdempotencyPort;
import com.sixpay.payment.application.port.output.initiation.PaymentInitiationPreparationPort;
import com.sixpay.payment.application.port.output.initiation.PreparedPaymentInitiation;
import com.sixpay.payment.application.exception.PaymentCustomerVerificationRetryableException;
import com.sixpay.payment.application.view.PaymentInitiationResult;
import com.sixpay.payment.application.view.PaymentConfirmationView;
import com.sixpay.payment.domain.model.ConfirmationChallengeStatus;
import com.sixpay.payment.domain.model.IdempotencyKey;
import com.sixpay.payment.domain.model.FailureCategory;
import com.sixpay.payment.domain.model.FailureCode;
import com.sixpay.payment.domain.model.FailureStage;
import com.sixpay.payment.domain.model.PaymentFailure;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.RetryDisposition;
import com.sixpay.payment.domain.model.PaymentStatus;
import com.sixpay.payment.domain.policy.PaymentPolicyBundle;
import com.sixpay.sharedkernel.domain.valueobject.Money;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.Objects;

/** Synchronous Payment initiation through an ACTIVE confirmation challenge. */
@Service
public class PaymentInitiationOrchestrationService implements PaymentInitiationUseCase {
    private final PaymentInitiationIdempotencyPort idempotencyPort;
    private final PaymentInitiationPreparationPort preparationPort;
    private final PaymentReceptionService receptionService;
    private final PaymentMutationCoordinator coordinator;
    private final ObjectProvider<PaymentCustomerVerificationService> customerProvider;
    private final ObjectProvider<PaymentConfirmationService> confirmationProvider;
    private final PaymentPolicyBundle policies;
    private final TimeProvider timeProvider;
    private final PaymentInitiationDeadline initiationDeadline;

    public PaymentInitiationOrchestrationService(PaymentInitiationIdempotencyPort idempotencyPort, PaymentInitiationPreparationPort preparationPort, PaymentReceptionService receptionService, PaymentMutationCoordinator coordinator, ObjectProvider<PaymentCustomerVerificationService> customerProvider, ObjectProvider<PaymentConfirmationService> confirmationProvider, PaymentPolicyBundle policies, TimeProvider timeProvider, PaymentInitiationDeadline initiationDeadline) {
        this.idempotencyPort=Objects.requireNonNull(idempotencyPort); this.preparationPort=Objects.requireNonNull(preparationPort); this.receptionService=Objects.requireNonNull(receptionService); this.coordinator=Objects.requireNonNull(coordinator); this.customerProvider=Objects.requireNonNull(customerProvider); this.confirmationProvider=Objects.requireNonNull(confirmationProvider); this.policies=Objects.requireNonNull(policies); this.timeProvider=Objects.requireNonNull(timeProvider); this.initiationDeadline=Objects.requireNonNull(initiationDeadline);
    }
    @Override public PaymentInitiationResult initiatePayment(InitiatePaymentCommand command) {
        Objects.requireNonNull(command); return idempotencyPort.execute(command, hash -> initiateNew(command, hash));
    }
    private PaymentInitiationResult initiateNew(InitiatePaymentCommand command, String requestHash) {
        Instant receivedAt=timeProvider.now();
        PreparedPaymentInitiation prepared=preparationPort.prepare(command,requestHash,receivedAt);
        PaymentWorkflowResult received=receptionService.receive(prepared.paymentId(),prepared.publicPaymentReference(),prepared.intent(),prepared.receivedAt());
        if(received.status()!=PaymentStatus.RECEIVED) throw new IllegalStateException("Payment initiation must first durably persist Payment in RECEIVED");
        failIfDeadlineExpired(received.paymentId(),prepared.receivedAt());
        PaymentWorkflowResult pending=coordinator.mutate(received.paymentId(), payment -> { if(payment.status()==PaymentStatus.RECEIVED) payment.startBankingVerification(timeProvider.now()); });
        if(pending.status()!=PaymentStatus.BANKING_VERIFICATION_PENDING) throw new IllegalStateException("Payment initiation must enter BANKING_VERIFICATION_PENDING");
        PaymentCustomerVerificationService customer=customerProvider.getIfAvailable();
        if(customer==null) throw new IllegalStateException("Customer Verification bridge is unavailable");
        PaymentWorkflowResult verified;
        try {
            verified=customer.verifyCustomer(
                    received.paymentId(),
                    timeProvider.now(),
                    initiationDeadline.deadlineAt(prepared.receivedAt()),
                    policies
            );
        } catch (PaymentCustomerVerificationRetryableException failure) {
            if (initiationDeadline.expired(prepared.receivedAt(), timeProvider.now())) {
                failInitiationDeadline(received.paymentId(), timeProvider.now());
            }
            throw failure;
        }
        failIfDeadlineExpired(received.paymentId(),prepared.receivedAt());
        if(verified.status()!=PaymentStatus.PENDING_CONFIRMATION) throw new IllegalStateException("Payment initiation requires VERIFIED banking preparation before confirmation creation; actual="+verified.status());
        PaymentConfirmationService confirmation=confirmationProvider.getIfAvailable();
        if(confirmation==null) throw new IllegalStateException("Payment Confirmation bridge is unavailable");
        failIfDeadlineExpired(received.paymentId(),prepared.receivedAt());
        PaymentConfirmationView challenge=confirmation.createBefore(
                new CreatePaymentConfirmationCommand(
                        received.publicPaymentReference(),
                        command.correlationId(),
                        IdempotencyKey.of(command.idempotencyKey())
                ),
                initiationDeadline.deadlineAt(prepared.receivedAt()),
                timeProvider
        );
        if(challenge.status()!=ConfirmationChallengeStatus.ACTIVE) throw new IllegalStateException("Payment initiation may return only after an ACTIVE confirmation challenge");
        return PaymentInitiationResult.awaitingOtp(received.paymentId(),received.publicPaymentReference(),command.endToEndId(),Money.of(command.totalAmount(),command.currency()),prepared.receivedAt(),challenge);
    }

    private void failIfDeadlineExpired(PaymentId paymentId, Instant receivedAt) {
        Instant now=timeProvider.now();
        if(!initiationDeadline.expired(receivedAt,now)) return;
        failInitiationDeadline(paymentId, now);
    }

    private void failInitiationDeadline(PaymentId paymentId, Instant failedAt) {
        PaymentFailure failure=new PaymentFailure(
                FailureCode.of("INITIATION_DEADLINE_EXCEEDED"),
                FailureCategory.TECHNICAL_FAILURE,
                FailureStage.BANKING_VERIFICATION,
                RetryDisposition.OPERATOR_ACTION_REQUIRED,
                "Payment initiation deadline exceeded",
                failedAt,
                null
        );
        coordinator.mutate(paymentId,payment -> payment.failInitiationDeadline(failure,failedAt));
        throw new IllegalStateException("Payment initiation deadline exceeded");
    }
}
