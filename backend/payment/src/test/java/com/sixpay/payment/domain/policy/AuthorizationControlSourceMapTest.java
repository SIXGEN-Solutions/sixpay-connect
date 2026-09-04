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
    void subscriptionAuthorizationNeverUsesLocalCustomerSubscription() {
        AuthorizationControlSource source =
                AuthorizationControlSourceMap.sourceFor(
                        AuthorizationControl.SUBSCRIPTION_AUTHORIZED
                );

        assertEquals("TRESOR_PAY", source.owner());
        assertEquals(
                AuthorizationSourceKind.TRUSTED_INTAKE_ATTESTATION,
                source.sourceKind()
        );
        assertTrue(source.evidence().contains("signed JWT"));
        assertTrue(
                source.evidence().contains(
                        "CustomerSubscription is not the source"
                )
        );
        assertTrue(
                source.evidence().contains(
                        "no synchronous TRESOR PAY subscription verification"
                )
        );
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
                AuthorizationControl.PARTNER_AUTHORIZED,
                AuthorizationControl.SUBSCRIPTION_AUTHORIZED,
                AuthorizationControl.APPLICATION_AUTHORIZED,
                AuthorizationControl.CLAIM_TYPE_AUTHORIZED,
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
