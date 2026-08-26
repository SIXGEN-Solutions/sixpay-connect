package com.sixpay.bootstrap.readiness;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentAuditQueryContractTest {

    private static final Path ROOT =
            Path.of("../..").normalize();

    @Test
    void openApiAndSpringMvcExposeSameAuditOperations()
            throws Exception {

        String contract = Files.readString(
                ROOT.resolve(
                        "documentation/contracts/internal/"
                                + "payment-audit-query-api-v1.yaml"
                )
        );
        String queryController = Files.readString(
                ROOT.resolve(
                        "backend/reporting/src/main/java/"
                                + "com/sixpay/reporting/api/controller/"
                                + "PaymentAuditQueryController.java"
                )
        );
        String exportController = Files.readString(
                ROOT.resolve(
                        "backend/reporting/src/main/java/"
                                + "com/sixpay/reporting/api/controller/"
                                + "PaymentAuditExportController.java"
                )
        );

        for (String path : List.of(
                "/internal/api/v1/payments/{paymentId}/timeline:",
                "/internal/api/v1/payment-audit-records:",
                "/internal/api/v1/payment-audit-records/{auditId}:",
                "/internal/api/v1/payment-audit-exports:",
                "/internal/api/v1/payment-audit-exports/{exportId}:"
        )) {
            assertTrue(
                    contract.contains(path),
                    () -> "Missing OpenAPI operation: " + path
            );
        }

        assertTrue(queryController.contains(
                "/internal/api/v1/payments/{paymentId}/timeline"
        ));
        assertTrue(queryController.contains(
                "/internal/api/v1/payment-audit-records"
        ));
        assertTrue(exportController.contains(
                "/internal/api/v1/payment-audit-exports"
        ));

        assertTrue(contract.contains("payment.audit.read"));
        assertTrue(contract.contains("payment.audit.export"));
        assertTrue(exportController.contains(
                "SCOPE_payment.audit.read"
        ));
        assertTrue(exportController.contains(
                "SCOPE_payment.audit.export"
        ));
        assertTrue(exportController.contains("Idempotency-Key"));
        assertTrue(exportController.contains(
                "ResponseEntity.accepted()"
        ));
        assertTrue(exportController.contains(
                "HttpHeaders.LOCATION"
        ));
    }
}
