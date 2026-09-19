package com.sixpay.payment.application.view;

import com.sixpay.payment.domain.model.ConfirmationChallengeStatus;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.PublicPaymentReference;
import com.sixpay.sharedkernel.domain.valueobject.Money;
import java.time.Instant;
import java.util.Objects;

/** Stable provider-neutral application result for Payment initiation. */
public record PaymentInitiationResult(
        PaymentId paymentId,
        PublicPaymentReference paymentReference,
        String endToEndId,
        Money totalAmount,
        Instant initiatedAt,
        PaymentInitiationStatus status,
        PaymentConfirmationView confirmationChallenge
) {
    public PaymentInitiationResult {
        paymentId = Objects.requireNonNull(paymentId, "Payment ID");
        paymentReference = Objects.requireNonNull(paymentReference, "Payment reference");
        if (endToEndId == null || endToEndId.isBlank()) throw new IllegalArgumentException("End-to-end ID must not be blank");
        endToEndId = endToEndId.trim();
        totalAmount = Objects.requireNonNull(totalAmount, "Total amount");
        initiatedAt = Objects.requireNonNull(initiatedAt, "Initiated instant");
        status = Objects.requireNonNull(status, "InitiateDebit status");
        confirmationChallenge = Objects.requireNonNull(confirmationChallenge, "Confirmation challenge");
        if (status != PaymentInitiationStatus.AWAITING_OTP) throw new IllegalArgumentException("InitiateDebit result must be AWAITING_OTP");
        if (confirmationChallenge.status() != ConfirmationChallengeStatus.ACTIVE) throw new IllegalArgumentException("AWAITING_OTP requires an ACTIVE confirmation challenge");
        if (!paymentReference.equals(confirmationChallenge.paymentReference())) throw new IllegalArgumentException("Confirmation challenge must belong to the initiated Payment");
    }
    public static PaymentInitiationResult awaitingOtp(PaymentId paymentId, PublicPaymentReference paymentReference, String endToEndId, Money totalAmount, Instant initiatedAt, PaymentConfirmationView challenge) {
        return new PaymentInitiationResult(paymentId, paymentReference, endToEndId, totalAmount, initiatedAt, PaymentInitiationStatus.AWAITING_OTP, challenge);
    }
}
