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
    void t1DocumentationReflectsCurrentAccountingT1ClosureBaseline() throws Exception {
        Path repoRoot = Path.of("..").toAbsolutePath().normalize().getParent();
        Path ai = repoRoot.resolve(
                "documentation/ai/accounting/ACCOUNTING_T1_AI_CONTEXT.md"
        );
        String text = Files.readString(ai);

        assertTrue(
                text.contains(
                        "documentation/contracts/amplitude/amplitude-accounting-entries-api-v1.yaml"
                )
        );
        assertTrue(
                text.contains(
                        "documentation/contracts/amplitude/amplitude-end-of-day-confirmation-api-v1.yaml"
                )
        );
        assertTrue(text.contains("POST /api/v1/accounting-entries"));
        assertTrue(
                text.contains(
                        "Unknown submission outcomes require authoritative lookup before any retry"
                )
        );
        assertTrue(text.contains("PENDING` -> persisted, never final"));
        assertTrue(
                text.contains(
                        "UNMATCHED`, quarantined, no Payment update"
                )
        );
        assertTrue(
                text.contains(
                        "AMBIGUOUS`, quarantined, no Payment update"
                )
        );
        assertTrue(
                text.contains(
                        "sixpay.accounting.tfj.finality.pending"
                )
        );
        assertTrue(
                text.contains(
                        "ACCOUNTING_T1 may be reported `REPOSITORY_VALIDATED / CLOSED` only after"
                )
        );
        assertTrue(text.contains("## Forbidden"));
    }
}
