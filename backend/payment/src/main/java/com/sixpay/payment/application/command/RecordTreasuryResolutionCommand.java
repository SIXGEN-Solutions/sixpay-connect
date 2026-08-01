package com.sixpay.payment.application.command;

import com.sixpay.payment.domain.model.PaymentFailure;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.TreasuryAccountReference;
import com.sixpay.payment.domain.model.evidence.TreasuryAccountResolutionSnapshot;
import com.sixpay.payment.domain.policy.PaymentPolicyBundle;

import java.time.Instant;
import java.util.Objects;

public record RecordTreasuryResolutionCommand(
        PaymentId paymentId,
        TreasuryAccountResolutionSnapshot evidence,
        TreasuryAccountReference resolvedAccount,
        PaymentFailure failure,
        Instant decisionAt,
        PaymentPolicyBundle policies
) {
    public RecordTreasuryResolutionCommand {
        paymentId = Objects.requireNonNull(paymentId, "Payment ID");
        evidence = Objects.requireNonNull(
                evidence,
                "Treasury-resolution evidence"
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
