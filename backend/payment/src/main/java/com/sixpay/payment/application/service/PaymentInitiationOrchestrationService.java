package com.sixpay.payment.application.service;

import com.sixpay.common.time.TimeProvider;
import com.sixpay.payment.application.command.CreatePaymentConfirmationCommand;
import com.sixpay.payment.application.command.InitiateDebitCommand;
import com.sixpay.payment.application.port.input.PaymentInitiationUseCase;
import com.sixpay.payment.application.port.output.idempotency.PaymentInitiationIdempotencyPort;
import com.sixpay.payment.application.port.output.initiation.PaymentInitiationPreparationPort;
import com.sixpay.payment.application.port.output.initiation.PreparedPaymentInitiation;
import com.sixpay.payment.application.view.InitiateDebitResult;
import com.sixpay.payment.application.view.PaymentConfirmationView;
import com.sixpay.payment.domain.model.ConfirmationChallengeStatus;
import com.sixpay.payment.domain.model.IdempotencyKey;
import com.sixpay.payment.domain.model.PaymentStatus;
import com.sixpay.payment.domain.policy.PaymentPolicyBundle;
import com.sixpay.sharedkernel.domain.valueobject.Money;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.Objects;

/** Synchronous InitiateDebit preparation through an ACTIVE OTP challenge. */
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

    public PaymentInitiationOrchestrationService(PaymentInitiationIdempotencyPort idempotencyPort, PaymentInitiationPreparationPort preparationPort, PaymentReceptionService receptionService, PaymentMutationCoordinator coordinator, ObjectProvider<PaymentCustomerVerificationService> customerProvider, ObjectProvider<PaymentConfirmationService> confirmationProvider, PaymentPolicyBundle policies, TimeProvider timeProvider) {
        this.idempotencyPort=Objects.requireNonNull(idempotencyPort); this.preparationPort=Objects.requireNonNull(preparationPort); this.receptionService=Objects.requireNonNull(receptionService); this.coordinator=Objects.requireNonNull(coordinator); this.customerProvider=Objects.requireNonNull(customerProvider); this.confirmationProvider=Objects.requireNonNull(confirmationProvider); this.policies=Objects.requireNonNull(policies); this.timeProvider=Objects.requireNonNull(timeProvider);
    }
    @Override public InitiateDebitResult initiateDebit(InitiateDebitCommand command) {
        Objects.requireNonNull(command); return idempotencyPort.execute(command, hash -> initiateNew(command, hash));
    }
    private InitiateDebitResult initiateNew(InitiateDebitCommand command, String requestHash) {
        Instant receivedAt=timeProvider.now();
        PreparedPaymentInitiation prepared=preparationPort.prepare(command,requestHash,receivedAt);
        PaymentWorkflowResult received=receptionService.receive(prepared.paymentId(),prepared.publicPaymentReference(),prepared.intent(),prepared.receivedAt());
        if(received.status()!=PaymentStatus.RECEIVED) throw new IllegalStateException("InitiateDebit must first durably persist Payment in RECEIVED");
        PaymentWorkflowResult pending=coordinator.mutate(received.paymentId(), payment -> { if(payment.status()==PaymentStatus.RECEIVED) payment.startBankingVerification(timeProvider.now()); });
        if(pending.status()!=PaymentStatus.BANKING_VERIFICATION_PENDING) throw new IllegalStateException("InitiateDebit must enter BANKING_VERIFICATION_PENDING");
        PaymentCustomerVerificationService customer=customerProvider.getIfAvailable();
        if(customer==null) throw new IllegalStateException("Customer Verification bridge is unavailable");
        PaymentWorkflowResult verified=customer.verifyCustomer(received.paymentId(),timeProvider.now(),policies);
        if(verified.status()!=PaymentStatus.PENDING_CONFIRMATION) throw new IllegalStateException("InitiateDebit requires VERIFIED banking preparation before OTP creation; actual="+verified.status());
        PaymentConfirmationService confirmation=confirmationProvider.getIfAvailable();
        if(confirmation==null) throw new IllegalStateException("Payment Confirmation bridge is unavailable");
        PaymentConfirmationView challenge=confirmation.create(new CreatePaymentConfirmationCommand(received.publicPaymentReference(),command.correlationId(),IdempotencyKey.of(command.idempotencyKey())));
        if(challenge.status()!=ConfirmationChallengeStatus.ACTIVE) throw new IllegalStateException("InitiateDebit may return only after an ACTIVE confirmation challenge");
        return InitiateDebitResult.awaitingOtp(received.paymentId(),received.publicPaymentReference(),command.endToEndId(),Money.of(command.totalAmount(),command.currency()),prepared.receivedAt(),challenge);
    }
}
