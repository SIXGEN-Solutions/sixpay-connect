package com.sixpay.payment.application.service;

import com.sixpay.common.time.TimeProvider;
import com.sixpay.payment.application.command.CreatePaymentConfirmationCommand;
import com.sixpay.payment.application.command.InitiateDebitCommand;
import com.sixpay.payment.application.port.input.PaymentInitiationUseCase;
import com.sixpay.payment.application.port.output.idempotency.PaymentInitiationIdempotencyPort;
import com.sixpay.payment.application.port.output.initiation.PaymentInitiationPreparationPort;
import com.sixpay.payment.application.port.output.initiation.PreparedPaymentInitiation;
import com.sixpay.payment.application.exception.PaymentCustomerVerificationRetryableException;
import com.sixpay.payment.application.view.InitiateDebitResult;
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

/**
 * Synchronous InitiateDebit preparation through an ACTIVE OTP challenge.
 */
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
        this.idempotencyPort = Objects.requireNonNull(idempotencyPort);
        this.preparationPort = Objects.requireNonNull(preparationPort);
        this.receptionService = Objects.requireNonNull(receptionService);
        this.coordinator = Objects.requireNonNull(coordinator);
        this.customerProvider = Objects.requireNonNull(customerProvider);
        this.confirmationProvider = Objects.requireNonNull(confirmationProvider);
        this.policies = Objects.requireNonNull(policies);
        this.timeProvider = Objects.requireNonNull(timeProvider);
        this.initiationDeadline = Objects.requireNonNull(initiationDeadline);
    }

    @Override
    public InitiateDebitResult initiateDebit(InitiateDebitCommand command) {
        Objects.requireNonNull(command);
        return idempotencyPort.execute(command, hash -> initiateNew(command, hash));
    }

    private InitiateDebitResult initiateNew(InitiateDebitCommand command, String requestHash) {
        var existing = receptionService.findExisting(command.applicationId(), command.endToEndId());
        PaymentId paymentId;
        com.sixpay.payment.domain.model.PublicPaymentReference reference;
        Instant receivedAt;
        PaymentStatus status;
        if (existing.isPresent()) {
            var payment = existing.orElseThrow();
            if (!payment.toState().requestIdentity().requestFingerprint().value().equals(requestHash)
                    || !payment.toState().requestIdentity().idempotencyKey().value()
                            .equals(command.idempotencyKey()))
                throw new IllegalStateException("Initiation retry does not match original Payment identity");
            paymentId = payment.id();
            reference = payment.publicPaymentReference();
            receivedAt = payment.toState().receivedAt();
            status = payment.status();
        } else {
            Instant now = timeProvider.now();
            PreparedPaymentInitiation prepared = preparationPort.prepare(command, requestHash, now);
            PaymentWorkflowResult received = receptionService.receive(
                    prepared.paymentId(), prepared.publicPaymentReference(), prepared.intent(), prepared.receivedAt());
            if (received.status() != PaymentStatus.RECEIVED)
                throw new IllegalStateException("InitiateDebit requires persisted RECEIVED");
            paymentId = received.paymentId();
            reference = received.publicPaymentReference();
            receivedAt = prepared.receivedAt();
            status = PaymentStatus.RECEIVED;
        }
        // All Payment persistence is committed by its independent adapter before I/O.
        if (status != PaymentStatus.PENDING_CONFIRMATION)
            failIfDeadlineExpired(paymentId, receivedAt);
        if (status == PaymentStatus.RECEIVED) {
            PaymentWorkflowResult pending = coordinator.mutate(paymentId, payment -> {
                if (payment.status() == PaymentStatus.RECEIVED)
                    payment.startBankingVerification(timeProvider.now());
            });
            status = pending.status();
        }
        if (status == PaymentStatus.BANKING_VERIFICATION_PENDING) {
            PaymentCustomerVerificationService customer = customerProvider.getIfAvailable();
            if (customer == null) throw new IllegalStateException("Customer Verification bridge unavailable");
            PaymentWorkflowResult verified;
            try {
                verified = customer.verifyCustomer(paymentId, timeProvider.now(),
                        initiationDeadline.deadlineAt(receivedAt), policies);
            } catch (PaymentCustomerVerificationRetryableException failure) {
                if (initiationDeadline.expired(receivedAt, timeProvider.now()))
                    failInitiationDeadline(paymentId, timeProvider.now());
                throw failure;
            }
            status = verified.status();
        }
        if (status != PaymentStatus.PENDING_CONFIRMATION)
            throw new IllegalStateException("Initiation requires verified banking preparation, actual=" + status);
        PaymentConfirmationService confirmation = confirmationProvider.getIfAvailable();
        if (confirmation == null) throw new IllegalStateException("Payment Confirmation bridge unavailable");
        CreatePaymentConfirmationCommand confirmationCommand =
                new CreatePaymentConfirmationCommand(reference, command.correlationId(),
                        IdempotencyKey.of(command.idempotencyKey()));
        PaymentConfirmationView challenge = initiationDeadline.expired(receivedAt, timeProvider.now())
                ? confirmation.recoverInitiation(confirmationCommand)
                : confirmation.createBefore(confirmationCommand,
                        initiationDeadline.deadlineAt(receivedAt), timeProvider);
        if (challenge.status() != ConfirmationChallengeStatus.ACTIVE)
            throw new IllegalStateException("Initiation requires ACTIVE challenge");
        return InitiateDebitResult.awaitingOtp(paymentId, reference, command.endToEndId(),
                Money.of(command.totalAmount(), command.currency()), receivedAt, challenge);
    }

    private void failIfDeadlineExpired(PaymentId paymentId, Instant receivedAt) {
        Instant now = timeProvider.now();
        if (!initiationDeadline.expired(receivedAt, now)) return;
        failInitiationDeadline(paymentId, now);
    }

    private void failInitiationDeadline(PaymentId paymentId, Instant failedAt) {
        PaymentFailure failure = new PaymentFailure(
                FailureCode.of("INITIATION_DEADLINE_EXCEEDED"),
                FailureCategory.TECHNICAL_FAILURE,
                FailureStage.BANKING_VERIFICATION,
                RetryDisposition.OPERATOR_ACTION_REQUIRED,
                "Payment initiation deadline exceeded",
                failedAt,
                null
        );
        coordinator.mutate(paymentId, payment -> payment.failInitiationDeadline(failure, failedAt));
        throw new IllegalStateException("Payment initiation deadline exceeded");
    }
}
