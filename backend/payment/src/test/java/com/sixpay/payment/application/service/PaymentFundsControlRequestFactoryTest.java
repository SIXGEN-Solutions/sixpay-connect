package com.sixpay.payment.application.service;

import com.sixpay.payment.domain.model.PaymentAggregateTestFixtures;
import com.sixpay.payment.domain.model.PaymentStatus;
import com.sixpay.payment.domain.model.evidence.FundsControlCheckType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaymentFundsControlRequestFactoryTest {

    private final PaymentFundsControlRequestFactory factory =
            new PaymentFundsControlRequestFactory();

    @Test
    void buildsRequestOnlyFromFundsControlPendingPaymentState() {
        var state =
                PaymentAggregateTestFixtures.sixpayAuthorizedPayment()
                        .toState();
        Instant requestedAt =
                Instant.parse("2026-09-05T00:00:00Z");

        var request = factory.create(state, requestedAt);

        assertEquals(PaymentStatus.FUNDS_CONTROL_PENDING, state.status());
        assertEquals(state.paymentId(), request.paymentId());
        assertEquals(
                state.publicPaymentReference(),
                request.publicPaymentReference()
        );
        assertEquals(
                state.financialInstitutionCode(),
                request.financialInstitutionCode()
        );
        assertEquals(state.requestedAmount(), request.requestedAmount());
        assertEquals(
                state.debtorAccountReference().bindingFingerprint(),
                request.debtorAccountBindingFingerprint()
        );
        assertEquals(
                Set.of(FundsControlCheckType.values()),
                request.requiredChecks()
        );
        assertEquals(requestedAt, request.requestedAt());
    }

    @Test
    void refusesRequestBeforeFundsControlPending() {
        var state =
                PaymentAggregateTestFixtures.newPayment()
                        .toState();

        assertThrows(
                IllegalArgumentException.class,
                () -> factory.create(
                        state,
                        Instant.parse("2026-09-05T00:00:00Z")
                )
        );
    }
}
