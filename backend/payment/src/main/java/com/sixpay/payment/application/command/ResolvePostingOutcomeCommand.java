package com.sixpay.payment.application.command;

import com.sixpay.payment.domain.model.PaymentFailure;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.evidence.PostingOutcomeSnapshot;
import com.sixpay.payment.domain.policy.PaymentPolicyBundle;

import java.time.Instant;
import java.util.Objects;

public record ResolvePostingOutcomeCommand(
        PaymentId paymentId,
        PostingOutcomeSnapshot authoritativeEvidence,
        PaymentFailure failure,
        Instant decisionAt,
        PaymentPolicyBundle policies
) {
    public ResolvePostingOutcomeCommand {
        paymentId = Objects.requireNonNull(paymentId, "Payment ID");
        authoritativeEvidence = Objects.requireNonNull(
                authoritativeEvidence,
                "Authoritative posting evidence"
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
