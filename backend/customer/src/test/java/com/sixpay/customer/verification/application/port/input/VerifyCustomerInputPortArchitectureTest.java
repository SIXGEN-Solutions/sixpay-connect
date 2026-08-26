package com.sixpay.customer.verification.application.port.input;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class VerifyCustomerInputPortArchitectureTest {

    private static final Path ROOT = Path.of(
            "src/main/java/com/sixpay/customer/verification/application/port/input"
    );

    @Test
    void remainsFrameworkFreeAndCustomerNative() throws Exception {
        try (var paths = Files.walk(ROOT)) {
            var violations = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .flatMap(path -> {
                        try {
                            String source = Files.readString(path);
                            return List.of(
                                            "import org.springframework.",
                                            "import jakarta.",
                                            "import com.sixpay.payment.",
                                            "import com.sixpay.customer.verification.infrastructure.",
                                            "Amplitude",
                                            "RestClient",
                                            "HttpClient",
                                            "HttpStatus",
                                            "AmplitudeClientException",
                                            "DebtorAccountReference",
                                            "BankingVerificationSnapshot"
                                    )
                                    .stream()
                                    .filter(source::contains)
                                    .map(token -> path + " contains " + token);
                        } catch (Exception exception) {
                            throw new IllegalStateException(exception);
                        }
                    })
                    .toList();

            assertTrue(
                    violations.isEmpty(),
                    () -> "Input-port boundary violations: " + violations
            );
        }
    }
}
