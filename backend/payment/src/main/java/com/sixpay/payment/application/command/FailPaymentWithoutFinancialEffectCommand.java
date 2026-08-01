package com.sixpay.payment.application.command;

import com.sixpay.payment.domain.model.PaymentFailure;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.policy.PaymentPolicyBundle;

import java.time.Instant;
import java.util.Objects;

public record FailPaymentWithoutFinancialEffectCommand(
        PaymentId paymentId,
        PaymentFailure failure,
        Instant finalizedAt,
        PaymentPolicyBundle policies
) {
    public FailPaymentWithoutFinancialEffectCommand {
        paymentId = Objects.requireNonNull(paymentId, "Payment ID");
        failure = Objects.requireNonNull(
                failure,
                "Technical failure"
        );
        finalizedAt = Objects.requireNonNull(
                finalizedAt,
                "Finalization instant"
        );
        policies = Objects.requireNonNull(
                policies,
                "Payment policies"
        );
    }
}
