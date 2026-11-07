package com.sixpay.payment.infrastructure.banking.amplitude.posting;

import com.sixpay.payment.domain.model.evidence.EvidenceCheckResult;
import com.sixpay.payment.domain.model.evidence.FundsControlCheckType;
import com.sixpay.payment.domain.model.evidence.PaymentEventOutcome;
import com.sixpay.payment.infrastructure.banking.amplitude.posting.dto.AmplitudePaymentEventResult;
import com.sixpay.payment.infrastructure.banking.amplitude.posting.dto.AmplitudePaymentExecutionCheck;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AmplitudePaymentEventAdapterChecksTest {

    @Test
    void mapsCoreBankingAccountValidAndTheThreeExecutionChecksWithoutAlias() {
        AmplitudePaymentEventResult result = new AmplitudePaymentEventResult(
                "PAY-01ARZ3NDEKTSV4RRFFQ69G5FAV",
                "COMPLETED",
                List.of(
                        new AmplitudePaymentExecutionCheck("ACCOUNT_VALID", "PASS", null),
                        new AmplitudePaymentExecutionCheck("DEBIT_ALLOWED", "PASS", null),
                        new AmplitudePaymentExecutionCheck("AVAILABLE_FUNDS_SUFFICIENT", "PASS", null)
                ),
                "787/000002", null,
                Instant.parse("2026-11-06T02:36:13Z"),
                LocalDate.of(2026, 11, 5)
        );

        var mapped = AmplitudePaymentEventAdapter.toApplicationResult(result);
        assertEquals(PaymentEventOutcome.COMPLETED, mapped.outcome());
        assertEquals(3, mapped.checks().size());
        assertEquals(FundsControlCheckType.ACCOUNT_VALID, mapped.checks().get(0).type());
        assertEquals(EvidenceCheckResult.PASS, mapped.checks().get(0).result());
        assertEquals("787/000002", mapped.bankReference());
    }
}
