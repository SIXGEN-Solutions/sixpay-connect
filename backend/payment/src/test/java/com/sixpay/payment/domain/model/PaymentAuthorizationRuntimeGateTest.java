package com.sixpay.payment.domain.model;

import com.sixpay.payment.domain.policy.*;
import org.junit.jupiter.api.Test;
import java.util.Set;
import static com.sixpay.payment.domain.model.PaymentAggregateTestFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class PaymentAuthorizationRuntimeGateTest {
    private final SixpayAuthorizationGate gate = new SixpayAuthorizationGate();
    private final PartnerAuthorizationView active =
            new PartnerAuthorizationView(true, Set.of(ClaimType.AVI, ClaimType.IM7, ClaimType.RNF));

    @Test void approvedRuntimeEvidenceResolvesBothFormerlyMissingControls() {
        var state = authorizationCheckingPayment().toState();
        var result = gate.evaluate(state, active, new PaymentAuthorizationRuntimeView(true, true));
        assertEquals(SixpayAuthorizationGateResult.Outcome.APPROVED, result.outcome());
        assertEquals(AuthorizationControlOutcome.PASS,
                result.resultFor(AuthorizationControl.SUBSCRIPTION_AUTHORIZED).outcome());
        assertEquals(AuthorizationControlOutcome.PASS,
                result.resultFor(AuthorizationControl.APPLICATION_AUTHORIZED).outcome());
    }

    @Test void suspendedSubscriptionBlocksFinancialApprovalWithoutPretendingItIsMissing() {
        var result = gate.evaluate(authorizationCheckingPayment().toState(), active,
                new PaymentAuthorizationRuntimeView(false, true));
        assertTrue(result.rejected());
        assertEquals(AuthorizationControlOutcome.FAIL,
                result.resultFor(AuthorizationControl.SUBSCRIPTION_AUTHORIZED).outcome());
    }

    @Test void unregisteredMachineBlocksFinancialApproval() {
        var result = gate.evaluate(authorizationCheckingPayment().toState(), active,
                new PaymentAuthorizationRuntimeView(true, false));
        assertTrue(result.rejected());
    }

    @Test void missingRuntimeEvidenceRemainsUnresolved() {
        var result = gate.evaluate(authorizationCheckingPayment().toState(), active,
                new PaymentAuthorizationRuntimeView(null, null));
        assertTrue(result.incomplete());
    }
}
