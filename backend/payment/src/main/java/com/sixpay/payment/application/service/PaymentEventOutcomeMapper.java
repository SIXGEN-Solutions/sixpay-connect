package com.sixpay.payment.application.service;

import com.sixpay.payment.application.port.output.banking.PaymentEventExecutionPort;
import com.sixpay.payment.domain.model.BankPostingReference;
import com.sixpay.payment.domain.model.evidence.FundsControlCheckEvidence;
import com.sixpay.payment.domain.model.evidence.PaymentEventObservationSource;
import com.sixpay.payment.domain.model.evidence.PaymentEventOutcomeSnapshot;
import com.sixpay.payment.domain.policy.PostingInstructionIdentity;

import java.time.LocalDate;
import java.util.Objects;

final class PaymentEventOutcomeMapper {

    PaymentEventOutcomeSnapshot toSnapshot(
            PostingInstructionIdentity instruction,
            PaymentEventExecutionPort.PaymentEventExecutionResult result,
            LocalDate accountingDate,
            PaymentEventObservationSource observationSource
    ) {
        Objects.requireNonNull(instruction, "Posting instruction");
        Objects.requireNonNull(result, "Payment event result");
        Objects.requireNonNull(accountingDate, "Accounting date");
        Objects.requireNonNull(observationSource, "Observation source");

        return new PaymentEventOutcomeSnapshot(
                instruction.instructionId(),
                instruction.idempotencyKey(),
                result.outcome(),
                result.checks().stream()
                        .map(check ->
                                new FundsControlCheckEvidence(
                                        check.type(),
                                        check.result(),
                                        check.reasonCode(),
                                        result.observedAt()
                                )
                        )
                        .toList(),
                result.bankReference() == null
                        ? null
                        : BankPostingReference.principalOnly(result.bankReference()),
                result.reasonCode(),
                accountingDate,
                result.observedAt(),
                observationSource
        );
    }
}
