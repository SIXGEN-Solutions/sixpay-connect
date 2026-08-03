package com.sixpay.customer.verification.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BankingVerificationPortArchitectureTest {

    private static final Path PORT_ROOT = Path.of(
            "src/main/java/com/sixpay/customer/verification/application/port/out"
    );

    @Test
    void portBoundaryContainsNoFrameworkExternalOrPaymentType()
            throws Exception {

        try (var paths = Files.walk(PORT_ROOT)) {
            var violations = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .flatMap(path -> {
                        try {
                            String source = Files.readString(path);
                            return List.of(
                                            "import org.springframework.",
                                            "import jakarta.",
                                            "import java.net.",
                                            "import com.sixpay.payment.",
                                            "RestClient",
                                            "WebClient",
                                            "HttpClient",
                                            "HttpHeaders",
                                            "HttpServlet",
                                            "@Service",
                                            "@Component",
                                            "@Repository"
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
                    () -> "Port-boundary violations: " + violations
            );
        }
    }

    @Test
    void noEndpointOrExternalDtoIsInvented() throws Exception {
        Path banking = Path.of(
                "src/main/java/com/sixpay/customer/verification/infrastructure/banking"
        );

        try (var paths = Files.walk(banking)) {
            for (Path path : paths.filter(Files::isRegularFile).toList()) {
                String source = Files.readString(path);

                for (String forbidden : List.of(
                        "http://",
                        "https://",
                        "/api/",
                        "RestClient",
                        "AmplitudeCustomerVerificationRequest",
                        "AmplitudeCustomerVerificationResponse"
                )) {
                    assertFalse(
                            source.contains(forbidden),
                            () -> "Invented contract found: " + forbidden
                    );
                }
            }
        }
    }
}
