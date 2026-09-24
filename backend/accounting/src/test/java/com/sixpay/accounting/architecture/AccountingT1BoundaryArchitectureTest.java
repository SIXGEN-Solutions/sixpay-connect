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

}
