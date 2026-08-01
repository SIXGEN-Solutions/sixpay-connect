package com.sixpay.payment.application.command;

import com.sixpay.payment.domain.model.PaymentFailure;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.evidence.FundsControlSnapshot;
import com.sixpay.payment.domain.policy.PaymentPolicyBundle;

import java.time.Instant;
import java.util.Objects;

public record RecordFundsControlCommand(
        PaymentId paymentId,
        FundsControlSnapshot evidence,
        PaymentFailure failure,
        Instant decisionAt,
        PaymentPolicyBundle policies
) {
    public RecordFundsControlCommand {
        paymentId = Objects.requireNonNull(paymentId, "Payment ID");
        evidence = Objects.requireNonNull(
                evidence,
                "Funds-control evidence"
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
