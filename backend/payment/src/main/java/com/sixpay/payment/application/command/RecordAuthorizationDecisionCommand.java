package com.sixpay.payment.application.command;

import com.sixpay.payment.domain.model.PaymentFailure;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.evidence.AuthorizationEvidenceSnapshot;
import com.sixpay.payment.domain.policy.PaymentPolicyBundle;

import java.time.Instant;
import java.util.Objects;

public record RecordAuthorizationDecisionCommand(
        PaymentId paymentId,
        AuthorizationEvidenceSnapshot evidence,
        PaymentFailure rejectionFailure,
        Instant decisionAt,
        PaymentPolicyBundle policies
) {
    public RecordAuthorizationDecisionCommand {
        paymentId = Objects.requireNonNull(paymentId, "Payment ID");
        evidence = Objects.requireNonNull(
                evidence,
                "Authorization evidence"
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
