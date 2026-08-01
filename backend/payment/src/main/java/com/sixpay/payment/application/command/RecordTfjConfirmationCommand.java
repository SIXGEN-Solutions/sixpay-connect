package com.sixpay.payment.application.command;

import com.sixpay.payment.domain.model.PaymentFailure;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.evidence.EndOfDayConfirmationSnapshot;

import com.sixpay.payment.domain.policy.PaymentPolicyBundle;
import com.sixpay.payment.domain.policy.UniqueTfjMatchProof;

import java.time.Instant;
import java.util.Objects;

public record RecordTfjConfirmationCommand(
        PaymentId paymentId,
        EndOfDayConfirmationSnapshot evidence,
        UniqueTfjMatchProof matchProof,
        PaymentFailure reconciliationFailure,
        Instant decisionAt,
        PaymentPolicyBundle policies
) {
    public RecordTfjConfirmationCommand {
        paymentId = Objects.requireNonNull(paymentId, "Payment ID");
        evidence = Objects.requireNonNull(
                evidence,
                "TFJ evidence"
        );
        matchProof = Objects.requireNonNull(
                matchProof,
                "Unique TFJ match proof"
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
