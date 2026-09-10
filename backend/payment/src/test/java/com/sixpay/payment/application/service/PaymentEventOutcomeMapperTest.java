package com.sixpay.payment.application.service;

import com.sixpay.payment.application.port.output.banking.PaymentEventExecutionPort;
import com.sixpay.payment.domain.model.evidence.*;
import com.sixpay.payment.domain.policy.PostingInstructionIdentity;
import com.sixpay.sharedkernel.domain.valueobject.Money;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaymentEventOutcomeMapperTest {

    @Test
    void mapsCompletedProviderResultToAtomicEvidence() {
        PostingInstructionIdentity instruction =
                new PostingInstructionIdentity(
                        new PostingInstructionId(
                                UUID.fromString(
                                        "80579bb5-af3c-46bd-a2fc-d30fb2672aed"
                                )
                        ),
                        new PostingIdempotencyKey(
                                "POSTING-IDEMPOTENCY-001"
                        ),
                        Money.of(
                                new BigDecimal("1000"),
                                "XAF"
                        ),
                        "v1:" + "a".repeat(64),
                        EvidenceFingerprint.of(
                                "v1:sha256:" + "1".repeat(64)
                        )
                );

        PaymentEventExecutionPort.PaymentEventExecutionResult result =
                new PaymentEventExecutionPort.PaymentEventExecutionResult(
                        "PAY-01J8YH6M6VT8EF3Z7Q4N9P2KDC",
                        PaymentEventOutcome.COMPLETED,
                        Arrays.stream(
                                        FundsControlCheckType.values()
                                )
                                .map(type ->
                                        new PaymentEventExecutionPort.PaymentEventExecutionCheck(
                                                type,
                                                EvidenceCheckResult.PASS,
                                                null
                                        )
                                )
                                .toList(),
                        "POSTING-001",
                        null,
                        Instant.parse(
                                "2026-09-06T23:55:00Z"
                        )
                );

        PaymentEventOutcomeSnapshot snapshot =
                new PaymentEventOutcomeMapper().toSnapshot(
                        instruction,
                        result,
                        LocalDate.of(2026, 9, 6),
                        PaymentEventObservationSource.DIRECT_RESPONSE
                );

        assertEquals(
                PaymentEventOutcome.COMPLETED,
                snapshot.outcome()
        );
        assertEquals(8, snapshot.checks().size());
        assertEquals(
                "POSTING-001",
                snapshot.bankPostingReference()
                        .orElseThrow()
                        .principalPostingReference()
        );
    }
}
