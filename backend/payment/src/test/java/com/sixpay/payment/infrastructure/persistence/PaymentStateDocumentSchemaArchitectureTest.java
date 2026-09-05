package com.sixpay.payment.infrastructure.persistence;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentStateDocumentSchemaArchitectureTest {

    private static final Path DOCUMENT =
            Path.of(
                    "src/main/java/com/sixpay/payment/"
                            + "infrastructure/persistence/"
                            + "PaymentStateDocument.java"
            );

    @Test
    void stateDocumentUsesVersionFiveAndGuardsLegacyPayloads()
            throws Exception {

        String source = Files.readString(DOCUMENT);

        assertTrue(source.contains(
                "CURRENT_SCHEMA_VERSION = 5"
        ));

        assertTrue(source.contains(
                "validateSchemaCompatibility()"
        ));

        /*
         * Schema v1 remains readable but must not contain fields
         * introduced by later Payment-state schemas.
         */
        assertTrue(source.contains(
                "Legacy Payment state payload must not contain"
        ));

        /*
         * Schema v2 remains backward compatible but must never contain
         * ConfirmationChallenge, which is introduced only by schema v3.
         */
        assertTrue(source.contains(
                "schemaVersion == 2"
        ));

        assertTrue(source.contains(
                "confirmationChallenge != null"
        ));

        assertTrue(source.contains(
                "Payment state schema version 2 must not contain"
        ));

        /*
         * Schema v2 preserves the legacy confirmation representation.
         */
        assertTrue(source.contains(
                "schemaVersion == 2"
        ));

        assertTrue(source.contains(
                "requires initiation context for"
        ));

        assertTrue(source.contains(
                "Payment state schema version 2 requires confirmation"
        ));

        /*
         * Starting with schema v3, a VERIFIED ConfirmationChallenge is a
         * valid post-confirmation proof and BANKING_VERIFICATION_PENDING
         * remains a legitimate pre-confirmation state.
         */
        assertTrue(source.contains(
                "schemaVersion >= 3"
        ));

        assertTrue(source.contains(
                "requiresVerifiedConfirmation(status)"
        ));

        assertTrue(source.contains(
                "BANKING_VERIFICATION_PENDING"
        ));

        assertTrue(source.contains(
                "ConfirmationChallengeStatus.VERIFIED"
        ));

        assertTrue(source.contains(
                "confirmationChallenge.verifiedAt() != null"
        ));

        assertTrue(source.contains(
                "PaymentInitiationContext initiationContext"
        ));

        assertTrue(source.contains(
                "CustomerConfirmationEvidence customerConfirmationEvidence"
        ));

        /*
         * LOT 1.2 — ConfirmationChallenge becomes subordinate persisted
         * state starting with PaymentStateDocument schema v3.
         */
        assertTrue(source.contains(
                "ConfirmationChallenge confirmationChallenge"
        ));

        assertTrue(source.contains(
                ".confirmationChallenge(confirmationChallenge)"
        ));

        /*
         * LOT 1.R4 — schema v4 persists the canonical Customer Verification
         * banking references as part of BankingVerificationSnapshot.
         */
        assertTrue(source.contains(
                "schemaVersion >= 4"
        ));

        assertTrue(source.contains(
                "customerReferenceOptional()"
        ));

        assertTrue(source.contains(
                "accountReferenceOptional()"
        ));

        assertTrue(source.contains(
                "requires canonical banking customer/account"
        ));

        /*
         * LOT 2.1.6 - schema v5 persists the Payment-owned local SIXPAY
         * authorization decision while preserving legacy external evidence.
         */
        assertTrue(source.contains(
                "SixpayAuthorizationDecisionSnapshot sixpayAuthorizationDecision"
        ));

        assertTrue(source.contains(
                "schemaVersion < 5"
        ));

        assertTrue(source.contains(
                "must not contain a SIXPAY authorization decision"
        ));

        assertTrue(source.contains(
                ".sixpayAuthorizationDecision(sixpayAuthorizationDecision)"
        ));
    }
}