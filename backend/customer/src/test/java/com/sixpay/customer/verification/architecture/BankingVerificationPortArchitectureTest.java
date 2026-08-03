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

    private static final Path BANKING_ROOT = Path.of(
            "src/main/java/com/sixpay/customer/verification/infrastructure/banking"
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
                                            "Amplitude",
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
    void bankingInfrastructureContainsTheApprovedAdapterLayers()
            throws Exception {

        for (String required : List.of(
                "AmplitudeCustomerVerificationAdapter.java",
                "client/AmplitudeCustomerVerificationClient.java",
                "dto/AmplitudeCustomerVerificationRequest.java",
                "dto/AmplitudeCustomerVerificationResponse.java",
                "mapper/AmplitudeCustomerVerificationMapper.java",
                "error/AmplitudeErrorResponse.java",
                "configuration/AmplitudeCustomerVerificationConfiguration.java",
                "configuration/BankingVerificationProperties.java"
        )) {
            assertTrue(
                    Files.isRegularFile(BANKING_ROOT.resolve(required)),
                    () -> "Missing approved banking infrastructure file: "
                            + required
            );
        }
    }

    @Test
    void endpointAndExternalDtosRemainConfinedToInfrastructure()
            throws Exception {

        Path customerRoot = Path.of(
                "src/main/java/com/sixpay/customer"
        );

        try (var paths = Files.walk(customerRoot)) {
            var violations = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.startsWith(BANKING_ROOT))
                    .flatMap(path -> {
                        try {
                            String source = Files.readString(path);
                            return List.of(
                                            "AmplitudeCustomerVerificationRequest",
                                            "AmplitudeCustomerVerificationResponse",
                                            "AmplitudeVerificationCheckResponse",
                                            "AmplitudeErrorResponse",
                                            "RestClient",
                                            "/v1/accounts/verify"
                                    )
                                    .stream()
                                    .filter(source::contains)
                                    .map(token -> path + " leaks " + token);
                        } catch (Exception exception) {
                            throw new IllegalStateException(exception);
                        }
                    })
                    .toList();

            assertTrue(
                    violations.isEmpty(),
                    () -> "Banking infrastructure leakage: " + violations
            );
        }
    }
}
