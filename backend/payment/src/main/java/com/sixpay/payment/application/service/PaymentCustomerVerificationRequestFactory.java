package com.sixpay.payment.application.service;

import com.sixpay.payment.application.port.output.CustomerVerificationRequest;
import com.sixpay.payment.domain.model.Payment;
import com.sixpay.payment.domain.model.PaymentInitiationContext;
import com.sixpay.payment.domain.model.PaymentState;
import com.sixpay.payment.domain.model.PaymentStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Builds the Payment-owned Customer Verification request from aggregate state.
 */
@Component
public final class PaymentCustomerVerificationRequestFactory {

    public CustomerVerificationRequest from(
            Payment payment,
            UUID verificationId,
            Instant requestedAt
    ) {
        Objects.requireNonNull(payment, "payment is required");
        Objects.requireNonNull(
                verificationId,
                "verificationId is required"
        );
        Objects.requireNonNull(requestedAt, "requestedAt is required");

        if (payment.status()
                != PaymentStatus.BANKING_VERIFICATION_PENDING) {
            throw new IllegalStateException(
                    "Customer verification requires "
                            + "BANKING_VERIFICATION_PENDING, actual="
                            + payment.status()
            );
        }

        PaymentState state = payment.toState();
        PaymentInitiationContext initiationContext =
                state.initiationContext()
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Payment initiation context is required"
                                )
                        );

        return new CustomerVerificationRequest(
                verificationId,
                initiationContext.taxpayerIdentifier(),
                initiationContext.debtorName(),
                state.financialInstitutionCode().value(),
                state.debtorAccountReference()
                        .bindingFingerprint(),
                state.debtorAccountReference()
                        .integrationAccountToken(),
                state.requestIdentity()
                        .correlationId()
                        .value(),
                state.paymentId().value(),
                requestedAt
        );
    }
}
