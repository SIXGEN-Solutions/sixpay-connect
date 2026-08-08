package com.sixpay.reporting.integration;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentAuditMaskingIT {

    @Test
    void reportingSchemaAndExportContainOnlyAllowListedEvidence()
            throws Exception {

        String migration = Files.readString(
                Path.of(
                        "src/test/resources/db/migration/"
                                + "V202608072058__create_"
                                + "reporting_payment_audit_projection.sql"
                )
        );

        String generator = Files.readString(
                Path.of(
                        "src/main/java/com/sixpay/reporting/"
                                + "infrastructure/export/"
                                + "JdbcPaymentAuditExportGenerator.java"
                )
        );

        for (String forbidden : List.of(
                "raw_niu",
                "clear_niu",
                "account_number",
                "iban",
                "secret",
                "api_key",
                "authorization_header"
        )) {
            assertFalse(
                    migration.toLowerCase().contains(forbidden)
            );
            assertFalse(
                    generator.toLowerCase().contains(forbidden)
            );
        }

        assertTrue(generator.contains("payment_reference"));
        assertTrue(generator.contains("observed_customer_id"));
        assertTrue(generator.contains("integrity_value"));
    }
}
