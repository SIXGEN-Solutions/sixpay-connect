package com.sixpay.payment.domain.policy;

import com.sixpay.payment.domain.model.*;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResultAndDisclosurePoliciesTest {

    @Test
    void resultIntentDependsOnAcceptedStatusChangeNotDelivery() {
        PolicyDecision<ResultIntentDecision> decision =
                new PaymentResultIntentPolicy().decide(
                        new PaymentResultContext(
                                PublicPaymentReference.of(
                                        "PAY-01J8YH6M6VT8EF3Z7Q4N9P2KDC"
                                ),
                                "correlation-001"
                        ),
                        PaymentStatus.POSTING_PENDING,
                        PaymentStatus.POSTED_PENDING_TFJ,
                        null,
                        PolicyTestFixtures.DECISION_AT,
                        PolicyTestFixtures.resultProfile()
                );

        assertEquals(
                ResultIntentDecision.IMMEDIATE_POSTED_PENDING_TFJ,
                decision.decision()
        );
    }

    @Test
    void disclosurePolicyAllowsOnlyDeclaredNonSensitiveFields() {
        PaymentEventDisclosurePolicy policy =
                new PaymentEventDisclosurePolicy();

        assertEquals(
                EventDisclosureDecision.ALLOW,
                policy.decide(
                        new ExplicitEventPayload(
                                "PaymentReceived",
                                Map.of(
                                        "paymentReference",
                                        "PAY-...",
                                        "status",
                                        "RECEIVED"
                                )
                        ),
                        PolicyTestFixtures.disclosureProfile()
                ).decision()
        );

        assertEquals(
                EventDisclosureDecision.REJECT_UNDECLARED_FIELD,
                policy.decide(
                        new ExplicitEventPayload(
                                "PaymentReceived",
                                Map.of("unexpected", "value")
                        ),
                        PolicyTestFixtures.disclosureProfile()
                ).decision()
        );
    }
}
