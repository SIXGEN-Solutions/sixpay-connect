package com.sixpay.payment.application.command;


import com.sixpay.payment.domain.model.PaymentFailure;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.evidence.ReversalSnapshot;
import com.sixpay.payment.domain.policy.PaymentPolicyBundle;


import java.time.Instant;
import java.util.Objects;

public record ResolveReversalOutcomeCommand(
        PaymentId paymentId,
        ReversalSnapshot authoritativeEvidence,
        PaymentFailure failure,
        Instant decisionAt,
        PaymentPolicyBundle policies
) {
    public ResolveReversalOutcomeCommand {
        paymentId = Objects.requireNonNull(paymentId, "Payment ID");
        authoritativeEvidence = Objects.requireNonNull(
                authoritativeEvidence,
                "Authoritative reversal evidence"
        );
        decisionAt = Objects.requireNonNull(
                decisionAt,
                "Decision instant"
        );
        policies = Objects.requireNonNull(
                policies,
                "Payment policies"
        );
    }
}
