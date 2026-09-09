package com.sixpay.accounting.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountingT1BoundaryArchitectureTest {

    @Test
    void accountingProductionCodeDoesNotAccessPaymentInternals() throws Exception {
        Path root = Path.of("src/main/java");
        try (Stream<Path> files = Files.walk(root)) {
            var sources = files
                    .filter(path -> path.toString().endsWith(".java"))
                    .map(path -> {
                        try { return Files.readString(path); }
                        catch (Exception e) { throw new RuntimeException(e); }
                    })
                    .toList();

            assertFalse(sources.stream().anyMatch(source ->
                    source.contains("com.sixpay.payment.infrastructure")
                            || source.contains("PaymentJpaEntity")
                            || source.contains("PaymentSpringDataRepository")
                            || source.contains("PaymentFinancialSnapshotRepository")));
        }
    }

    @Test
    void t1DocumentationReflectsApprovedProviderContractAndT15Implementation() throws Exception {
        Path repoRoot = Path.of("..").toAbsolutePath().normalize().getParent();
        Path ai = repoRoot.resolve(
                "documentation/ai/accounting/ACCOUNTING_T1_AI_CONTEXT.md"
        );
        String text = Files.readString(ai);

        assertTrue(
                text.contains(
                        "physical Core Banking T1 API is defined by the approved T1.4 contract"
                )
        );
        assertTrue(
                text.contains("## T1.5 active implementation context")
        );
        assertTrue(
                text.contains("POST /api/v1/accounting-entries")
        );
        assertTrue(
                text.contains(
                        "Blind replay after unknown provider submission outcome"
                )
        );
        assertTrue(text.contains("FORBIDDEN"));
    }
}
