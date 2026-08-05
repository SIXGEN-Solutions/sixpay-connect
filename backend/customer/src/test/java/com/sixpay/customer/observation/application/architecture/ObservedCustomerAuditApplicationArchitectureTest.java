package com.sixpay.customer.observation.application.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservedCustomerAuditApplicationArchitectureTest {

    private static final Path APPLICATION = Path.of(
            "src/main/java/com/sixpay/customer/observation/application"
    );

    private static final Path AUDIT =
            APPLICATION.resolve("audit");

    private static final Path AUDIT_PORT =
            APPLICATION.resolve("port/output/audit");

    @Test
    void auditPackageContainsOnlyTheApprovedContractTypes()
            throws Exception {
        assertEquals(
                Set.of(
                        "ObservedCustomerAuditAction.java",
                        "ObservedCustomerAuditOutcome.java",
                        "ObservedCustomerAuditRecord.java",
                        "ObservedCustomerAuditContext.java",
                        "package-info.java"
                ),
                javaFiles(AUDIT)
        );

        assertEquals(
                Set.of(
                        "ObservedCustomerAuditPort.java",
                        "package-info.java"
                ),
                javaFiles(AUDIT_PORT)
        );
    }

    @Test
    void auditContractIsFrameworkAndExternalDomainFree()
            throws Exception {
        assertNoTokens(
                List.of(AUDIT, AUDIT_PORT),
                List.of(
                        "import org.springframework.",
                        "import jakarta.persistence.",
                        "import org.hibernate.",
                        "import com.sixpay.payment.",
                        "Amplitude",
                        "Authentication",
                        "Jwt",
                        "HttpServlet",
                        "@Entity",
                        "@Component",
                        "@Service",
                        "@Repository"
                )
        );
    }

    @Test
    void auditRecordContainsNoSensitiveFieldVocabulary()
            throws Exception {
        String record = Files.readString(
                AUDIT.resolve(
                        "ObservedCustomerAuditRecord.java"
                )
        );

        for (String forbidden : List.of(
                "normalizedNiu",
                "legalName",
                "emailMasked",
                "phoneMasked",
                "accountNumber",
                "maskedAccountReference",
                "accountBindingFingerprint",
                "payload",
                "accessToken",
                "apiKey",
                "cursor"
        )) {
            assertFalse(
                    record.contains(forbidden),
                    () -> "Sensitive audit field: " + forbidden
            );
        }

        for (String required : List.of(
                "UUID auditId",
                "ObservedCustomerAuditAction action",
                "ObservedCustomerAuditOutcome outcome",
                "ObservedCustomerId observedCustomerId",
                "UUID sourceEventId",
                "UUID paymentId",
                "String actorId",
                "String correlationId",
                "Instant occurredAt",
                "String reasonCode"
        )) {
            assertTrue(
                    record.contains(required),
                    () -> "Missing audit field: " + required
            );
        }
    }

    @Test
    void portIsAppendOnlyAndHasNoReadUpdateOrDeleteOperation()
            throws Exception {
        String port = Files.readString(
                AUDIT_PORT.resolve(
                        "ObservedCustomerAuditPort.java"
                )
        );

        assertTrue(port.contains(
                "void append(ObservedCustomerAuditRecord record)"
        ));

        for (String forbidden : List.of(
                "find",
                "search",
                "update",
                "delete",
                "remove",
                "saveAll"
        )) {
            assertFalse(
                    port.contains(forbidden),
                    () -> "Non append-only operation: " + forbidden
            );
        }
    }

    private static Set<String> javaFiles(Path root)
            throws Exception {
        try (Stream<Path> paths = Files.list(root)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path ->
                            path.toString().endsWith(".java")
                    )
                    .map(path ->
                            path.getFileName().toString()
                    )
                    .collect(Collectors.toSet());
        }
    }

    private static void assertNoTokens(
            List<Path> roots,
            List<String> forbidden
    ) throws Exception {
        for (Path root : roots) {
            try (Stream<Path> paths = Files.walk(root)) {
                List<String> violations = paths
                        .filter(Files::isRegularFile)
                        .filter(path ->
                                path.toString().endsWith(".java")
                        )
                        .flatMap(path -> {
                            try {
                                String source =
                                        Files.readString(path);
                                return forbidden.stream()
                                        .filter(source::contains)
                                        .map(token ->
                                                path
                                                        + " contains "
                                                        + token
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
                        () -> "Audit architecture violations: "
                                + violations
                );
            }
        }
    }
}
