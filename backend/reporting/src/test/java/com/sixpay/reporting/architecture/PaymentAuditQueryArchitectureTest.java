package com.sixpay.reporting.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentAuditQueryArchitectureTest {

    private static final Path ROOT =
            Path.of("src/main/java/com/sixpay/reporting");

    @Test
    void lot64ReadControllerRemainsReadOnly()
            throws Exception {

        String controller = Files.readString(
                ROOT.resolve(
                        "api/controller/"
                                + "PaymentAuditQueryController.java"
                )
        );

        assertTrue(controller.contains(
                "/internal/api/v1/payments/{paymentId}/timeline"
        ));
        assertTrue(controller.contains(
                "/internal/api/v1/payment-audit-records"
        ));
        assertTrue(controller.contains(
                "/internal/api/v1/payment-audit-records/{auditId}"
        ));
        assertFalse(controller.contains("@PostMapping"));
        assertFalse(controller.contains("@PutMapping"));
        assertFalse(controller.contains("@DeleteMapping"));
    }

    @Test
    void queryRequiresAuditReadScope()
            throws Exception {

        String controller = Files.readString(
                ROOT.resolve(
                        "api/controller/"
                                + "PaymentAuditQueryController.java"
                )
        );

        assertTrue(controller.contains(
                "SCOPE_payment.audit.read"
        ));
    }

    @Test
    void reportingReadsOnlyItsOwnProjection()
            throws Exception {

        String adapter = Files.readString(
                ROOT.resolve(
                        "infrastructure/query/"
                                + "PaymentAuditProjectionReadAdapter.java"
                )
        );

        assertTrue(adapter.contains(
                "reporting_payment_audit_evidence"
        ));
        assertTrue(adapter.contains(
                "NamedParameterJdbcTemplate"
        ));

        for (String forbidden : List.of(
                "FROM payments ",
                "customer_observed_",
                "accounting_",
                "notification_",
                "import com.sixpay.payment.",
                "import com.sixpay.customer.",
                "import com.sixpay.accounting.",
                "import com.sixpay.notification."
        )) {
            assertFalse(
                    adapter.contains(forbidden),
                    () -> "Forbidden cross-domain query: "
                            + forbidden
            );
        }
    }

    @Test
    void applicationLayerRemainsFrameworkFree()
            throws Exception {

        try (var paths = Files.walk(
                ROOT.resolve("application")
        )) {
            List<String> violations = paths
                    .filter(Files::isRegularFile)
                    .filter(path ->
                            path.toString().endsWith(".java")
                    )
                    .flatMap(path -> {
                        try {
                            String source =
                                    Files.readString(path);
                            return List.of(
                                            "import org.springframework.",
                                            "import jakarta.persistence.",
                                            "EntityManager",
                                            "JdbcTemplate",
                                            "@Service",
                                            "@Component",
                                            "@Repository"
                                    )
                                    .stream()
                                    .filter(source::contains)
                                    .map(token ->
                                            path + " contains " + token
                                    );
                        } catch (Exception exception) {
                            throw new IllegalStateException(
                                    exception
                            );
                        }
                    })
                    .toList();

            assertTrue(
                    violations.isEmpty(),
                    () -> "Application violations: "
                            + violations
            );
        }
    }

    @Test
    void cursorIsAuthenticatedAndCarriesSnapshot()
            throws Exception {

        String codec = Files.readString(
                ROOT.resolve(
                        "infrastructure/query/"
                                + "HmacAuditCursorCodec.java"
                )
        );

        assertTrue(codec.contains("HmacSHA256"));
        assertTrue(codec.contains("MessageDigest.isEqual("));
        assertTrue(codec.contains("snapshotAt.toEpochMilli()"));
        assertTrue(codec.contains("cursor signature is invalid"));
    }
}
