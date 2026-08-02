package com.sixpay.payment.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentFinalArchitectureGateTest {

    private static final Path MAIN = Path.of(
            "src/main/java/com/sixpay/payment"
    );

    @Test
    void paymentModuleRemainsNonExecutable()
            throws IOException {
        String module = Files.readString(
                MAIN.resolve("PaymentModule.java")
        );

        assertFalse(module.contains("@SpringBootApplication"));
        assertFalse(module.contains("public static void main("));
    }

    @Test
    void domainHasNoFrameworkOrOuterLayerDependency()
            throws IOException {
        assertEquals(
                List.of(),
                violations(
                        MAIN.resolve("domain"),
                        List.of(
                                "org.springframework.",
                                "jakarta.persistence.",
                                "org.hibernate.",
                                "com.sixpay.payment.application.",
                                "com.sixpay.payment.infrastructure.",
                                "com.sixpay.payment.configuration."
                        )
                )
        );
    }

    @Test
    void applicationHasNoInfrastructureDependency()
            throws IOException {
        assertEquals(
                List.of(),
                violations(
                        MAIN.resolve("application"),
                        List.of(
                                "com.sixpay.payment.infrastructure."
                        )
                )
        );
    }

    @Test
    void restApiRemainsReadOnly()
            throws IOException {
        String source = Files.readString(
                MAIN.resolve(
                        "api/PaymentQueryController.java"
                )
        );

        assertTrue(source.contains(
                "@RequestMapping(\"/internal/api/v1/payments\")"
        ));

        for (String forbidden : Set.of(
                "@PostMapping",
                "@PutMapping",
                "@PatchMapping",
                "@DeleteMapping"
        )) {
            assertFalse(source.contains(forbidden));
        }
    }

    @Test
    void postingAndReconciliationRemainSeparated()
            throws IOException {
        String finalization = Files.readString(
                MAIN.resolve(
                        "application/service/"
                                + "PaymentFinalizationService.java"
                )
        );
        String reconciliation = Files.readString(
                MAIN.resolve(
                        "application/service/"
                                + "PaymentReconciliationService.java"
                )
        );

        assertFalse(finalization.contains(
                "recordMatchedEndOfDayConfirmation"
        ));
        assertFalse(finalization.contains(
                "EndOfDayConfirmationSnapshot"
        ));
        assertTrue(reconciliation.contains(
                "recordMatchedEndOfDayConfirmation"
        ));
        assertFalse(reconciliation.contains(
                "PostingGateway"
        ));
        assertFalse(reconciliation.contains(
                "recordPostingOutcome"
        ));
    }

    @Test
    void paymentHasNoDirectAccountingOrNotificationDependency()
            throws IOException {
        assertEquals(
                List.of(),
                violations(
                        MAIN,
                        List.of(
                                "com.sixpay.accounting.",
                                "com.sixpay.notification."
                        )
                )
        );
    }

    private static List<String> violations(
            Path root,
            List<String> forbidden
    ) throws IOException {
        try (Stream<Path> paths = Files.walk(root)) {
            return paths
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
                                            path + " contains " + token
                                    );
                        } catch (IOException exception) {
                            throw new IllegalStateException(
                                    exception
                            );
                        }
                    })
                    .toList();
        }
    }
}
