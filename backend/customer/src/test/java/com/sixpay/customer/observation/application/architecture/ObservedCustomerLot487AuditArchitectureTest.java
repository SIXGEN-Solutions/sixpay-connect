package com.sixpay.customer.observation.application.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservedCustomerLot487AuditArchitectureTest {

    private static final Path ROOT = Path.of(
            "src/main/java/com/sixpay/customer/observation"
    );

    @Test
    void projectionSuccessPathsAreAudited()
            throws Exception {

        String source = Files.readString(
                ROOT.resolve(
                        "application/service/audit/"
                                + "AuditedObserveCustomerUseCase.java"
                )
        );

        for (String required : List.of(
                "PROJECTION_APPLIED",
                "PROJECTION_REPLAYED",
                "PROJECTION_STALE_IGNORED",
                "PROJECTION_REJECTED",
                "ObservedCustomerAuditContext.system(",
                "command.correlationId()",
                "command.sourceEventId()",
                "command.paymentId()",
                "auditPort.append("
        )) {
            assertTrue(
                    source.contains(required),
                    () -> "Missing projection audit concept: "
                            + required
            );
        }
    }

    @Test
    void auditModelAndPortRemainFrameworkFree()
            throws Exception {

        for (Path path : List.of(
                ROOT.resolve("application/audit"),
                ROOT.resolve("application/port/output/audit")
        )) {
            assertNoTokens(
                    path,
                    List.of(
                            "import org.springframework.",
                            "import jakarta.persistence.",
                            "import org.hibernate.",
                            "import com.sixpay.payment.",
                            "Amplitude",
                            "@Entity",
                            "@Component",
                            "@Service",
                            "@Repository",
                            "@Transactional"
                    )
            );
        }
    }

    @Test
    void auditPersistenceRemainsAppendOnlyAndSensitiveFree()
            throws Exception {

        Path audit = ROOT.resolve("infrastructure/audit");

        assertNoTokens(
                audit,
                List.of(
                        "repository.delete",
                        "repository.deleteAll",
                        "repository.findAll",
                        "@Modifying",
                        " UPDATE ",
                        " DELETE ",
                        "normalizedNiu",
                        "legalName",
                        "email",
                        "phone",
                        "maskedAccountReference",
                        "accountBindingFingerprint",
                        "payload",
                        "jwt",
                        "apiKey",
                        "cursor"
                )
        );
    }

    private static void assertNoTokens(
            Path root,
            List<String> tokens
    ) throws Exception {

        try (var paths = Files.walk(root)) {
            List<String> violations = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .flatMap(path -> {
                        try {
                            String source = Files.readString(path);
                            return tokens.stream()
                                    .filter(source::contains)
                                    .map(token ->
                                            path + " contains " + token
                                    );
                        } catch (Exception exception) {
                            throw new IllegalStateException(exception);
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
