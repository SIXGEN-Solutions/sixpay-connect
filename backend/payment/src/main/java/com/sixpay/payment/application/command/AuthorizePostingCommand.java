package com.sixpay.payment.application.command;

import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.policy.PaymentPolicyBundle;
import com.sixpay.payment.domain.policy.PostingInstructionIdentity;

import java.time.Instant;
import java.util.Objects;

public record AuthorizePostingCommand(
        PaymentId paymentId,
        PostingInstructionIdentity instruction,
        Instant authorizedAt,
        PaymentPolicyBundle policies
) {
    public AuthorizePostingCommand {
        paymentId = Objects.requireNonNull(paymentId, "Payment ID");
        instruction = Objects.requireNonNull(
                instruction,
                "Posting instruction"
        );
        authorizedAt = Objects.requireNonNull(
                authorizedAt,
                "Posting authorization instant"
        );
        policies = Objects.requireNonNull(
                policies,
                "Payment policies"
        );
    }
}
