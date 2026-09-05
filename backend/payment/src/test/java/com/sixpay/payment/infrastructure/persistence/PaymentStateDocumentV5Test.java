package com.sixpay.payment.infrastructure.persistence;

import com.sixpay.payment.domain.model.Payment;
import com.sixpay.payment.domain.model.PaymentAggregateTestFixtures;
import com.sixpay.payment.domain.model.PaymentState;
import com.sixpay.payment.domain.model.PaymentStatus;
import com.sixpay.payment.domain.model.evidence.AuthorizationDecisionOutcome;
import com.sixpay.payment.domain.model.evidence.SixpayAuthorizationDecisionSnapshot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentStateDocumentV5Test {

    @Test
    void currentPaymentStateSchemaIsVersionFive() {
        assertEquals(
                5,
                PaymentStateDocument.CURRENT_SCHEMA_VERSION
        );
    }

    @Test
    void v5PreservesApprovedSixpayAuthorizationDecisionAcrossRoundTrip() {
        Payment payment =
                PaymentAggregateTestFixtures.sixpayAuthorizedPayment();

        SixpayAuthorizationDecisionSnapshot expectedDecision =
                payment.toState()
                        .sixpayAuthorizationDecision()
                        .orElseThrow();

        PaymentStateDocument document =
                PaymentStateDocument.from(payment.toState());

        PaymentState restored = document.toState();

        assertEquals(
                PaymentStatus.FUNDS_CONTROL_PENDING,
                restored.status()
        );

        SixpayAuthorizationDecisionSnapshot restoredDecision =
                restored.sixpayAuthorizationDecision()
                        .orElseThrow();

        assertEquals(
                AuthorizationDecisionOutcome.APPROVED,
                restoredDecision.outcome()
        );
        assertEquals(
                expectedDecision.decidedAt(),
                restoredDecision.decidedAt()
        );
        assertTrue(restoredDecision.approved());
    }
}
