package com.sixpay.payment.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentCustomerVerificationBoundaryTest {

    private static final Path PAYMENT_ROOT = Path.of(
            "src/main/java/com/sixpay/payment"
    );

    @Test
    void paymentContainsNoCustomerModuleDependency()
            throws Exception {

        try (var paths = Files.walk(PAYMENT_ROOT)) {
            var violations = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .flatMap(path -> {
                        try {
                            String source = Files.readString(path);

                            return source.contains(
                                    "import com.sixpay.customer."
                            )
                                    ? java.util.stream.Stream.of(
                                            path + " imports Customer"
                                    )
                                    : java.util.stream.Stream.empty();
                        } catch (Exception exception) {
                            throw new IllegalStateException(exception);
                        }
                    })
                    .toList();

            assertTrue(
                    violations.isEmpty(),
                    () -> "Customer dependency leaked into Payment: "
                            + violations
            );
        }
    }

    @Test
    void paymentOwnedPortRemainsFrameworkAndInfrastructureFree()
            throws Exception {

        Path portRoot = PAYMENT_ROOT.resolve(
                "application/port/output"
        );

        for (String fileName : List.of(
                "CustomerVerificationPort.java",
                "CustomerVerificationRequest.java",
                "CustomerVerificationResponse.java"
        )) {
            String source = Files.readString(
                    portRoot.resolve(fileName)
            );

            for (String forbidden : List.of(
                    "import com.sixpay.customer.",
                    "import org.springframework.",
                    "import com.sixpay.payment.infrastructure.",
                    "import java.net.http.",
                    "AmplitudeCustomerVerificationRequest",
                    "AmplitudeCustomerVerificationResponse",
                    "AmplitudeVerificationCheckResponse",
                    "AmplitudeErrorResponse",
                    "AmplitudeClientException",
                    "RestClient",
                    "WebClient",
                    "HttpClient",
                    "HttpStatus",
                    "HttpHeaders",
                    "VerifyCustomerUseCase"
            )) {
                assertFalse(
                        source.contains(forbidden),
                        () -> fileName
                                + " contains forbidden dependency "
                                + forbidden
                );
            }
        }
    }

    @Test
    void paymentPomDoesNotDependOnCustomer()
            throws Exception {

        String pom = Files.readString(Path.of("pom.xml"));

        assertFalse(
                pom.contains("<artifactId>customer</artifactId>"),
                "Payment Maven module must not depend on Customer"
        );
    }
}
