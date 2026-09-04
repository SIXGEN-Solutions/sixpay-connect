package com.sixpay.payment.domain.model;

import com.sixpay.payment.domain.policy.AuthorizationControl;
import com.sixpay.payment.domain.policy.AuthorizationControlOutcome;
import com.sixpay.payment.domain.policy.AuthorizationControlResult;
import com.sixpay.payment.domain.policy.SixpayAuthorizationGate;
import com.sixpay.payment.domain.policy.SixpayAuthorizationGateResult;
import com.sixpay.payment.domain.policy.PartnerAuthorizationView;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.EnumSet;

import static com.sixpay.payment.domain.model.PaymentAggregateTestFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class SixpayAuthorizationGateTest {

    private final SixpayAuthorizationGate gate =
            new SixpayAuthorizationGate();

    @Test
    void validPostOtpStateApprovesWhenAllSixAuthorizationControlsPass() {
        PaymentState state =
                authorizationCheckingPayment()
                        .toState()
                        .toBuilder()
                        .authorizationEvidence(
                                authorizationApproved("3")
                        )
                        .build();

        SixpayAuthorizationGateResult result =
                gate.evaluate(
                        state,
                        new PartnerAuthorizationView(
                                true,
                                java.util.Set.of(
                                        ClaimType.AVI,
                                        ClaimType.IM7,
                                        ClaimType.RNF
                                )
                        )
                );

        assertEquals(
                SixpayAuthorizationGateResult.Outcome.APPROVED,
                result.outcome()
        );

        assertEquals(
                AuthorizationControlOutcome.PASS,
                result.resultFor(
                        AuthorizationControl.REQUEST_DATA_CONSISTENT
                ).outcome()
        );

        assertEquals(
                AuthorizationControlOutcome.PASS,
                result.resultFor(
                        AuthorizationControl.SUBSCRIPTION_AUTHORIZED
                ).outcome()
        );
        assertEquals(
                AuthorizationControlOutcome.PASS,
                result.resultFor(
                        AuthorizationControl.APPLICATION_AUTHORIZED
                ).outcome()
        );

        for (AuthorizationControl control : EnumSet.of(
                AuthorizationControl.PARTNER_AUTHORIZED,
                AuthorizationControl.CLAIM_TYPE_AUTHORIZED,
                AuthorizationControl.EXECUTION_DATE_VALID
        )) {
            assertEquals(
                    AuthorizationControlOutcome.PASS,
                    result.resultFor(control).outcome(),
                    control.name()
            );
        }

        assertTrue(result.approved());
        assertFalse(result.rejected());
        assertFalse(result.incomplete());
    }

    @Test
    void inactivePartnerRejectsAuthorization() {
        PaymentState state = authorizationCheckingPayment().toState();

        SixpayAuthorizationGateResult result =
                gate.evaluate(
                        state,
                        new PartnerAuthorizationView(
                                false,
                                java.util.Set.of(
                                        ClaimType.AVI,
                                        ClaimType.IM7,
                                        ClaimType.RNF
                                )
                        )
                );

        assertEquals(
                AuthorizationControlOutcome.FAIL,
                result.resultFor(
                        AuthorizationControl.PARTNER_AUTHORIZED
                ).outcome()
        );
    }

    @Test
    void claimOutsideConfiguredPartnerPerimeterIsRejected() {
        PaymentState state = authorizationCheckingPayment().toState();

        SixpayAuthorizationGateResult result =
                gate.evaluate(
                        state,
                        new PartnerAuthorizationView(
                                true,
                                java.util.Set.of(ClaimType.IM7, ClaimType.RNF)
                        )
                );

        assertEquals(
                AuthorizationControlOutcome.FAIL,
                result.resultFor(
                        AuthorizationControl.CLAIM_TYPE_AUTHORIZED
                ).outcome()
        );
    }

    @Test
    void directPaymentExecutionDateHasNoLocalBankingCalendarRestriction() {
        PaymentState state = authorizationCheckingPayment().toState();

        SixpayAuthorizationGateResult result =
                gate.evaluate(
                        state,
                        new PartnerAuthorizationView(
                                true,
                                java.util.Set.of(
                                        ClaimType.AVI,
                                        ClaimType.IM7,
                                        ClaimType.RNF
                                )
                        )
                );

        assertEquals(
                AuthorizationControlOutcome.PASS,
                result.resultFor(
                        AuthorizationControl.EXECUTION_DATE_VALID
                ).outcome()
        );
    }

    @Test
    void nonAuthorizationCheckingStateFailsConsistencyControl() {
        PaymentState state = newPayment().toState();

        SixpayAuthorizationGateResult result =
                gate.evaluate(state);

        assertEquals(
                AuthorizationControlOutcome.FAIL,
                result.resultFor(
                        AuthorizationControl.REQUEST_DATA_CONSISTENT
                ).outcome()
        );
        assertEquals(
                SixpayAuthorizationGateResult.Outcome.REJECTED,
                result.outcome()
        );
    }

    @Test
    void resultRequiresExactlyTheSixControls() {
        EnumMap<
                AuthorizationControl,
                AuthorizationControlResult
        > partial = new EnumMap<>(AuthorizationControl.class);

        partial.put(
                AuthorizationControl.REQUEST_DATA_CONSISTENT,
                new AuthorizationControlResult(
                        AuthorizationControl.REQUEST_DATA_CONSISTENT,
                        AuthorizationControlOutcome.PASS,
                        "coherent"
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new SixpayAuthorizationGateResult(partial)
        );
    }
}
