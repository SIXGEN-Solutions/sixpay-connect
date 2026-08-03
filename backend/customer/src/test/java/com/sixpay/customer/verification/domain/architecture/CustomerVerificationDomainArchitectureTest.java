package com.sixpay.customer.verification.domain.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomerVerificationDomainArchitectureTest {

    private static final Path DOMAIN_ROOT = Path.of(
            "src/main/java/com/sixpay/customer/verification/domain"
    );

    @Test
    void domainRemainsFrameworkInfrastructureAndPaymentFree()
            throws IOException {

        assertSourcesDoNotContain(
                DOMAIN_ROOT,
                List.of(
                        "import org.springframework.",
                        "import jakarta.persistence.",
                        "import jakarta.servlet.",
                        "import org.hibernate.",
                        "import tools.jackson.",
                        "import java.net.",
                        "import java.sql.",
                        "import com.sixpay.payment.",
                        "import com.sixpay.customer.observation.",
                        "RestClient",
                        "WebClient",
                        "KafkaTemplate",
                        "EntityManager",
                        "JdbcTemplate"
                )
        );
    }

    @Test
    void domainNeverObtainsTheCurrentTime() throws IOException {
        assertSourcesDoNotContain(
                DOMAIN_ROOT,
                List.of(
                        "Instant.now(",
                        "LocalDate.now(",
                        "LocalDateTime.now(",
                        "OffsetDateTime.now(",
                        "ZonedDateTime.now(",
                        "System.currentTimeMillis(",
                        "System.nanoTime("
                )
        );
    }

    @Test
    void requestDoesNotContainTransportOrCredentialConcepts()
            throws IOException {

        String source = Files.readString(
                DOMAIN_ROOT.resolve(
                        "model/CustomerVerificationRequest.java"
                )
        );

        for (String forbidden : List.of(
                "import jakarta.servlet.",
                "HttpServletRequest",
                "HttpHeaders",
                "Authorization:",
                "Bearer ",
                "JwtDecoder",
                "JwtAuthenticationToken",
                "ApiKey",
                "APIKey",
                "AmplitudeClient",
                "import com.sixpay.payment."
        )) {
            assertFalse(
                    source.contains(forbidden),
                    () -> "Request contains forbidden concept: "
                            + forbidden
            );
        }
    }

    private static void assertSourcesDoNotContain(
            Path root,
            List<String> forbiddenTokens
    ) throws IOException {

        try (var paths = Files.walk(root)) {
            var violations = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .flatMap(path -> {
                        try {
                            String source = Files.readString(path);
                            return forbiddenTokens.stream()
                                    .filter(source::contains)
                                    .map(token -> path
                                            + " contains forbidden token "
                                            + token);
                        } catch (IOException exception) {
                            throw new IllegalStateException(exception);
                        }
                    })
                    .toList();

            assertTrue(
                    violations.isEmpty(),
                    () -> "Architecture violations: " + violations
            );
        }
    }
}
