package com.sixpay.payment.application.command;

import com.sixpay.payment.domain.model.PaymentFailure;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.authorization.SixpayAuthorizationDecisionSnapshot;
import com.sixpay.payment.domain.policy.PaymentPolicyBundle;

import java.time.Instant;
import java.util.Objects;

public record RecordSixpayAuthorizationDecisionCommand(
        PaymentId paymentId,
        SixpayAuthorizationDecisionSnapshot decision,
        PaymentFailure rejectionFailure,
        Instant decisionAt,
        PaymentPolicyBundle policies
) {
    public RecordSixpayAuthorizationDecisionCommand {
        paymentId = Objects.requireNonNull(paymentId, "Payment ID");
        decision = Objects.requireNonNull(
                decision,
                "SIXPAY authorization decision"
        );
        decisionAt = Objects.requireNonNull(
                decisionAt,
                "Authorization decision instant"
        );
        policies = Objects.requireNonNull(
                policies,
                "Payment policies"
        );
    }
}
