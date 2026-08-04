package com.sixpay.customer.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomerPaymentIndependenceArchitectureTest {

    private static final Path CUSTOMER_ROOT = Path.of(
            "src/main/java/com/sixpay/customer"
    );

    @Test
    void customerNeverDependsOnPayment() throws Exception {
        try (var paths = Files.walk(CUSTOMER_ROOT)) {
            var violations = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> {
                        try {
                            return Files.readString(path).contains(
                                    "import com.sixpay.payment."
                            );
                        } catch (Exception exception) {
                            throw new IllegalStateException(exception);
                        }
                    })
                    .toList();

            assertTrue(
                    violations.isEmpty(),
                    () -> "Customer imports Payment: " + violations
            );
        }
    }
}
