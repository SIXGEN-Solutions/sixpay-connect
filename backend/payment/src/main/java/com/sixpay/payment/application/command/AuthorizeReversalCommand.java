package com.sixpay.payment.application.command;

import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.evidence.ReversalAuthorizationEvidence;
import com.sixpay.payment.domain.policy.PaymentPolicyBundle;
import com.sixpay.payment.domain.policy.ReversalInstructionIdentity;

import java.time.Instant;
import java.util.Objects;

public record AuthorizeReversalCommand(
        PaymentId paymentId,
        ReversalInstructionIdentity instruction,
        ReversalAuthorizationEvidence authorization,
        Instant authorizedAt,
        PaymentPolicyBundle policies
) {
    public AuthorizeReversalCommand {
        paymentId = Objects.requireNonNull(paymentId, "Payment ID");
        instruction = Objects.requireNonNull(
                instruction,
                "Reversal instruction"
        );
        authorization = Objects.requireNonNull(
                authorization,
                "Reversal authorization"
        );
        authorizedAt = Objects.requireNonNull(
                authorizedAt,
                "Reversal authorization instant"
        );
        policies = Objects.requireNonNull(
                policies,
                "Payment policies"
        );
    }
}
