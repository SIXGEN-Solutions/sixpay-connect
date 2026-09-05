package com.sixpay.payment.application.model;

import com.sixpay.payment.domain.model.PaymentAggregateTestFixtures;
import com.sixpay.payment.domain.model.evidence.FundsControlCheckType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FundsControlRequestTest {

    @Test
    void requiresExactlyTheCanonicalEightFundsChecks() {
        var state =
                PaymentAggregateTestFixtures.sixpayAuthorizedPayment()
                        .toState();

        FundsControlRequest request = new FundsControlRequest(
                state.paymentId(),
                state.publicPaymentReference(),
                state.financialInstitutionCode(),
                state.requestedAmount(),
                state.debtorAccountReference().bindingFingerprint(),
                Set.of(FundsControlCheckType.values()),
                Instant.parse("2026-09-05T00:00:00Z")
        );

        assertEquals(
                Set.of(FundsControlCheckType.values()),
                request.requiredChecks()
        );
        assertEquals(8, request.requiredChecks().size());
    }

    @Test
    void rejectsAnIncompleteFundsCheckSet() {
        var state =
                PaymentAggregateTestFixtures.sixpayAuthorizedPayment()
                        .toState();

        assertThrows(
                IllegalArgumentException.class,
                () -> new FundsControlRequest(
                        state.paymentId(),
                        state.publicPaymentReference(),
                        state.financialInstitutionCode(),
                        state.requestedAmount(),
                        state.debtorAccountReference().bindingFingerprint(),
                        Set.of(FundsControlCheckType.ACCOUNT_EXISTS),
                        Instant.parse("2026-09-05T00:00:00Z")
                )
        );
    }
}
