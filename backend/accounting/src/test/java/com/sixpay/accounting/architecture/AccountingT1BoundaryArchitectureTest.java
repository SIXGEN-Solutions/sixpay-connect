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
    void t1DocumentationKeepsProviderContractUndefined() throws Exception {
        Path repoRoot = Path.of("..").toAbsolutePath().normalize().getParent();
        Path ai = repoRoot.resolve("documentation/ai/accounting/ACCOUNTING_T1_AI_CONTEXT.md");
        String text = Files.readString(ai);
        assertTrue(text.contains("Physical Core Banking Accounting API for T1.4"));
        assertTrue(text.contains("FORBIDDEN"));
    }
}
