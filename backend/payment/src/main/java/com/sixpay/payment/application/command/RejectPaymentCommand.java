package com.sixpay.payment.application.command;

import com.sixpay.payment.domain.model.PaymentFailure;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.policy.PaymentPolicyBundle;

import java.time.Instant;
import java.util.Objects;

public record RejectPaymentCommand(
        PaymentId paymentId,
        PaymentFailure rejection,
        Instant finalizedAt,
        PaymentPolicyBundle policies
) {
    public RejectPaymentCommand {
        paymentId = Objects.requireNonNull(paymentId, "Payment ID");
        rejection = Objects.requireNonNull(
                rejection,
                "Payment rejection"
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
