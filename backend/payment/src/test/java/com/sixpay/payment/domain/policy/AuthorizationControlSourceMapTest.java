package com.sixpay.payment.domain.policy;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthorizationControlSourceMapTest {

    @Test
    void mapsExactlyTheSixAuthorizationCheckingControls() {
        assertEquals(
                EnumSet.allOf(AuthorizationControl.class),
                AuthorizationControlSourceMap.all().keySet()
        );
        assertEquals(6, AuthorizationControlSourceMap.all().size());
    }

    @Test
    void subscriptionAuthorizationUsesCustomerOwnedLocalSubscription() {
        AuthorizationControlSource source = AuthorizationControlSourceMap.sourceFor(
                AuthorizationControl.SUBSCRIPTION_AUTHORIZED);
        assertEquals("customer", source.owner());
        assertEquals(AuthorizationSourceKind.REQUIRES_RUNTIME_SOURCE, source.sourceKind());
        assertTrue(source.evidence().contains("CustomerSubscription"));
        assertEquals(AuthorizationControlSource.ImplementationStatus.READY,
                source.implementationStatus());
    }

    @Test
    void requestConsistencyIsReadyFromPaymentOwnedState() {
        AuthorizationControlSource source =
                AuthorizationControlSourceMap.sourceFor(
                        AuthorizationControl.REQUEST_DATA_CONSISTENT
                );

        assertEquals("payment", source.owner());
        assertEquals(
                AuthorizationSourceKind.PAYMENT_STATE,
                source.sourceKind()
        );
        assertEquals(
                AuthorizationControlSource.ImplementationStatus.READY,
                source.implementationStatus()
        );

        assertTrue(source.evidence().contains("requestIdentity"));
        assertTrue(source.evidence().contains("requestedAmount"));
        assertTrue(source.evidence().contains("debtorAccountReference"));
        assertTrue(source.evidence().contains("allocationIntentFingerprint"));
        assertTrue(source.evidence().contains("bankingVerificationEvidence"));
        assertTrue(source.evidence().contains("confirmationChallenge"));
    }

    @Test
    void unresolvedControlsRemainExplicitlyBlockedForEvaluation() {
        for (AuthorizationControl control : EnumSet.of(
                AuthorizationControl.EXECUTION_DATE_VALID
        )) {
            AuthorizationControlSource source =
                    AuthorizationControlSourceMap.sourceFor(control);

            assertEquals(
                    AuthorizationControlSource
                            .ImplementationStatus
                            .REQUIRES_RUNTIME_SOURCE,
                    source.implementationStatus(),
                    control.name()
            );
            assertNotNull(source.owner());
            assertFalse(source.owner().isBlank());
            assertNotNull(source.evidence());
            assertFalse(source.evidence().isBlank());
        }
    }
}
