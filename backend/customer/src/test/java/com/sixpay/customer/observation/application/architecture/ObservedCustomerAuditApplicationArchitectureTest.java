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

    private static final Path APPLICATION_ROOT = Path.of(
            "src/main/java/com/sixpay/customer/observation/application"
    );

    private static final Path AUDIT_MODEL_ROOT =
            APPLICATION_ROOT.resolve("audit");

    private static final Path AUDIT_PORT_ROOT =
            APPLICATION_ROOT.resolve(
                    "port/output/audit"
            );

    @Test
    void auditPackageContainsOnlyTheApprovedContractTypes()
            throws Exception {

        Set<String> expectedAuditModel = Set.of(
                "ObservedCustomerAuditAction.java",
                "ObservedCustomerAuditOutcome.java",
                "ObservedCustomerAuditRecord.java",
                "ObservedCustomerAuditContext.java",
                "package-info.java"
        );

        Set<String> expectedAuditPorts = Set.of(
                "ObservedCustomerAuditPort.java",
                "ObservedCustomerAuditIdGenerator.java",
                "package-info.java"
        );

        assertEquals(
                expectedAuditModel,
                javaFiles(AUDIT_MODEL_ROOT)
        );

        assertEquals(
                expectedAuditPorts,
                javaFiles(AUDIT_PORT_ROOT)
        );
    }

    @Test
    void auditApplicationContractIsFrameworkAndExternalDomainFree()
            throws Exception {

        List<String> forbidden = List.of(
                "import org.springframework.",
                "import jakarta.persistence.",
                "import org.hibernate.",
                "import com.sixpay.payment.",
                "import com.sixpay.customer.observation.api.",
                "import com.sixpay.customer.observation.infrastructure.",
                "Authentication",
                "Jwt",
                "RestClient",
                "WebClient",
                "EntityManager",
                "JdbcTemplate",
                "@Entity",
                "@Repository",
                "@Service",
                "@Component",
                "@Transactional"
        );

        assertNoTokens(
                AUDIT_MODEL_ROOT,
                forbidden
        );

        assertNoTokens(
                AUDIT_PORT_ROOT,
                forbidden
        );
    }

    @Test
    void auditContractContainsNoSensitiveBusinessData()
            throws Exception {

        List<String> forbidden = List.of(
                "normalizedNiu",
                "legalName",
                "email",
                "phone",
                "accountNumber",
                "maskedAccountReference",
                "accountBindingFingerprint",
                "payload",
                "jwt",
                "apiKey",
                "cursor"
        );

        assertNoTokens(
                AUDIT_MODEL_ROOT,
                forbidden
        );

        assertNoTokens(
                AUDIT_PORT_ROOT,
                forbidden
        );
    }

    @Test
    void auditPortExposesAppendOnlyOperation()
            throws Exception {

        String source = Files.readString(
                AUDIT_PORT_ROOT.resolve(
                        "ObservedCustomerAuditPort.java"
                )
        );

        assertTrue(source.contains(
                "void append("
        ));

        for (String forbidden : List.of(
                "update(",
                "delete(",
                "remove(",
                "saveAll(",
                "deleteAll("
        )) {
            assertFalse(
                    source.contains(forbidden),
                    () -> "Forbidden audit mutation operation: "
                            + forbidden
            );
        }
    }

    private static Set<String> javaFiles(
            Path root
    ) throws Exception {

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
            Path root,
            List<String> forbidden
    ) throws Exception {

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
                                    "Cannot inspect " + path,
                                    exception
                            );
                        }
                    })
                    .toList();

            assertTrue(
                    violations.isEmpty(),
                    () -> "Audit application violations: "
                            + violations
            );
        }
    }
}