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
    void stateDocumentUsesVersionTwoAndGuardsLegacyPayloads()
            throws Exception {

        String source = Files.readString(DOCUMENT);

        assertTrue(source.contains(
                "CURRENT_SCHEMA_VERSION = 2"
        ));
        assertTrue(source.contains(
                "validateSchemaCompatibility()"
        ));
        assertTrue(source.contains(
                "Legacy Payment state payload must not contain"
        ));
        assertTrue(source.contains(
                "Payment state schema version 2 requires"
        ));
        assertTrue(source.contains(
                "PaymentInitiationContext initiationContext"
        ));
        assertTrue(source.contains(
                "CustomerConfirmationEvidence customerConfirmationEvidence"
        ));
    }
}
