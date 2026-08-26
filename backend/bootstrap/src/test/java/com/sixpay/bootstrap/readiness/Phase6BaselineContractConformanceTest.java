package com.sixpay.bootstrap.readiness;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Phase6BaselineContractConformanceTest {

    private static final Path REPOSITORY_ROOT = Path.of("../..");

    @Test
    void authoritativeInternalContractsExist() {
        for (String contract : List.of(
                "payment-query-api-v1.yaml",
                "observed-customer-query-api-v1.yaml",
                "payment-audit-query-api-v1.yaml"
        )) {
            Path path = REPOSITORY_ROOT.resolve(
                    "documentation/contracts/internal/" + contract
            );
            assertTrue(Files.isRegularFile(path),
                    () -> "Missing Phase 6 contract: " + path);
        }
    }

    @Test
    void paymentQueryContractBelongsToPaymentModule() throws Exception {
        String contract = Files.readString(REPOSITORY_ROOT.resolve(
                "documentation/contracts/internal/payment-query-api-v1.yaml"
        ));

        assertTrue(contract.contains("domain: payment"));
        assertTrue(contract.contains("capability: PAYMENT_QUERY"));
        assertTrue(contract.contains("/internal/api/v1/payments:"));

        Path controller = REPOSITORY_ROOT.resolve(
                "backend/payment/src/main/java/com/sixpay/payment/api/PaymentQueryController.java"
        );
        assertTrue(Files.isRegularFile(controller),
                "Payment Query controller must remain in Payment");
    }

    @Test
    void observedCustomerQueryContractBelongsToCustomerModule() throws Exception {
        String contract = Files.readString(REPOSITORY_ROOT.resolve(
                "documentation/contracts/internal/observed-customer-query-api-v1.yaml"
        ));

        assertTrue(contract.contains("domain: customer"));
        assertTrue(contract.contains("capability: OBSERVED_CUSTOMER_QUERY"));
        assertTrue(contract.contains("/internal/api/v1/observed-customers:"));

        Path controller = REPOSITORY_ROOT.resolve(
                "backend/customer/src/main/java/com/sixpay/customer/observation/api/controller/ObservedCustomerQueryController.java"
        );
        assertTrue(Files.isRegularFile(controller),
                "ObservedCustomer Query controller must remain in Customer");
    }

    @Test
    void paymentAuditContractBelongsToReporting() throws Exception {
        String contract = Files.readString(REPOSITORY_ROOT.resolve(
                "documentation/contracts/internal/payment-audit-query-api-v1.yaml"
        ));

        assertTrue(contract.contains("domain: reporting"));
        assertTrue(contract.contains("capability: PAYMENT_AUDIT_QUERY"));

        for (String operation : List.of(
                "/internal/api/v1/payments/{paymentId}/timeline:",
                "/internal/api/v1/payment-audit-records:",
                "/internal/api/v1/payment-audit-records/{auditId}:",
                "/internal/api/v1/payment-audit-exports:",
                "/internal/api/v1/payment-audit-exports/{exportId}:"
        )) {
            assertTrue(contract.contains(operation),
                    () -> "Missing audit contract operation: " + operation);
        }

        assertTrue(Files.isRegularFile(REPOSITORY_ROOT.resolve("backend/reporting/pom.xml")),
                "Reporting owning module must exist");
    }

    @Test
    void internalQueryApisAreNotOwnedByIntegrationModule() throws Exception {
        Path integrationRoot = REPOSITORY_ROOT.resolve("backend/integration");
        if (!Files.isDirectory(integrationRoot)) {
            return;
        }

        try (var paths = Files.walk(integrationRoot)) {
            var violations = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> {
                        try {
                            String source = Files.readString(path);
                            return source.contains("/internal/api/v1/payments")
                                    || source.contains("/internal/api/v1/observed-customers")
                                    || source.contains("/internal/api/v1/payment-audit-records")
                                    || source.contains("/internal/api/v1/payment-audit-exports");
                        } catch (Exception exception) {
                            throw new IllegalStateException(exception);
                        }
                    })
                    .toList();

            assertTrue(violations.isEmpty(),
                    () -> "Internal business query APIs leaked into integration: " + violations);
        }
    }

    @Test
    void phase6BaselineDocumentationExists() {
        Path baseline = REPOSITORY_ROOT.resolve(
                "documentation/architecture/internal/phase6-lot6.0-contract-conformance.md"
        );
        assertTrue(Files.isRegularFile(baseline),
                "Phase 6 baseline documentation is required");
    }

    @Test
    void baselineDoesNotClaimProductionReadiness() throws Exception {
        String baseline = Files.readString(REPOSITORY_ROOT.resolve(
                "documentation/architecture/internal/phase6-lot6.0-contract-conformance.md"
        ));
        assertFalse(baseline.contains("PRODUCTION_READY: true"));
        assertTrue(baseline.contains("LOT_6_0_BASELINE = COMPLETE"));
    }
}
