package com.sixpay.payment.infrastructure.audit;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

class PaymentAuditArchitectureTest {

    private static final Path AUDIT_ROOT = Path.of(
            "src/main/java/com/sixpay/payment/infrastructure/audit"
    );

    @Test
    void containsOnlyAuthorizedAuditTypes() throws IOException {
        Set<String> authorized = Set.of(
                "PaymentAuditAdapter.java",
                "PaymentAuditEntity.java",
                "PaymentAuditEntry.java",
                "PaymentAuditRepository.java",
                "package-info.java"
        );
        try (Stream<Path> paths = Files.list(AUDIT_ROOT)) {
            List<String> actual = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .map(path -> path.getFileName().toString())
                    .sorted()
                    .toList();
            assertEquals(authorized.stream().sorted().toList(), actual);
        }
    }

    @Test
    void writesRequireExistingTransaction() throws IOException {
        String source = Files.readString(
                AUDIT_ROOT.resolve("PaymentAuditAdapter.java")
        );
        assertTrue(source.contains("Propagation.MANDATORY"));
        assertFalse(source.contains("REQUIRES_NEW"));
    }
}
