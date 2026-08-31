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
    void stateDocumentUsesVersionFourAndGuardsLegacyPayloads()
            throws Exception {

        String source = Files.readString(DOCUMENT);

        assertTrue(source.contains(
                "CURRENT_SCHEMA_VERSION = 4"
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
         * Validation shared by schemas v2 and v3 must remain present
         * without hard-coding a version-specific error message.
         */
        assertTrue(source.contains(
                "schemaVersion >= 2"
        ));

        assertTrue(source.contains(
                "requires initiation context for"
        ));

        assertTrue(source.contains(
                "requires confirmation evidence after"
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
    }
}