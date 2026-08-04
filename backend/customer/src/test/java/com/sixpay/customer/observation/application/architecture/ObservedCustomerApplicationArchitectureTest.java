package com.sixpay.customer.observation.application.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservedCustomerApplicationArchitectureTest {

    private static final Path APPLICATION_ROOT = Path.of(
            "src/main/java/com/sixpay/customer/observation/application"
    );

    private static final Path PROJECTION_SERVICE =
            APPLICATION_ROOT.resolve(
                    "service/ObservedCustomerProjectionService.java"
            );

    @Test
    void applicationRemainsFrameworkAndExternalDomainFree()
            throws Exception {

        try (var paths = Files.walk(APPLICATION_ROOT)) {
            var violations = paths
                    .filter(Files::isRegularFile)
                    .filter(path ->
                            path.toString().endsWith(".java")
                    )
                    .flatMap(path -> {
                        try {
                            String source = Files.readString(path);

                            return List.of(
                                            "import com.sixpay.payment.",
                                            "import com.sixpay.customer.verification.",
                                            "import jakarta.persistence.",
                                            "import jakarta.transaction.",
                                            "import org.hibernate.",
                                            "import org.springframework.",
                                            "import java.net.",

                                            "import org.springframework.web.client.RestClient",
                                            "import org.springframework.web.reactive.function.client.WebClient",
                                            "import java.net.http.HttpClient",

                                            "AmplitudeCustomerVerificationClient",
                                            "AmplitudeCustomerVerificationRequest",
                                            "AmplitudeCustomerVerificationResponse",
                                            "AmplitudeVerificationCheckResponse",
                                            "AmplitudeErrorResponse",
                                            "CoreBankingAccessTokenProvider",
                                            "OAuth2CoreBankingAccessTokenProvider",

                                            "PaymentDomainEvent",

                                            "@Entity",
                                            "@MappedSuperclass",
                                            "@Embeddable",
                                            "@Repository",
                                            "@Service",
                                            "@Component",
                                            "@Transactional",

                                            "Instant.now(",
                                            "UUID.randomUUID("
                                    )
                                    .stream()
                                    .filter(source::contains)
                                    .map(token ->
                                            path
                                                    + " contains forbidden token "
                                                    + token
                                    );
                        } catch (Exception exception) {
                            throw new IllegalStateException(
                                    "Cannot inspect " + path,
                                    exception
                            );
                        }
                    })
                    .toList();

            assertTrue(
                    violations.isEmpty(),
                    () -> "Observed Customer application violations: "
                            + violations
            );
        }
    }

    @Test
    void projectionServiceUsesOnlyCustomerOwnedPorts()
            throws Exception {

        assertTrue(
                Files.isRegularFile(PROJECTION_SERVICE),
                () -> "Missing projection service: "
                        + PROJECTION_SERVICE
        );

        String source = Files.readString(
                PROJECTION_SERVICE
        );

        for (String required : List.of(
                "implements ObserveCustomerUseCase",
                "ObservedCustomerRepository",
                "ObservedPaymentRepository",
                "ObservedCustomerIdGenerator",
                "findByNormalizedNiu(",
                "ObservedCustomer.observeFirst(",
                "customer.observePayment(",
                "customerRepository.save(customer)",
                "paymentRepository.save("
        )) {
            assertTrue(
                    source.contains(required),
                    () -> "Missing projection orchestration: "
                            + required
            );
        }

        for (String forbidden : List.of(
                "import com.sixpay.payment.",
                "com.sixpay.payment.domain.model.PaymentStatus",
                "com.sixpay.payment.domain.model.PaymentFailure",
                "com.sixpay.payment.application.",
                "com.sixpay.payment.infrastructure.",

                "AmplitudeCustomerVerificationClient",
                "AmplitudeCustomerVerificationRequest",
                "AmplitudeCustomerVerificationResponse",
                "AmplitudeVerificationCheckResponse",
                "AmplitudeErrorResponse",

                "EntityManager",
                "JdbcTemplate",

                "import org.springframework.web.client.RestClient",
                "import org.springframework.web.reactive.function.client.WebClient",
                "import java.net.http.HttpClient",

                "@Transactional",
                "@Service",
                "@Component"
        )) {
            assertFalse(
                    source.contains(forbidden),
                    () -> "Forbidden projection dependency: "
                            + forbidden
            );
        }
    }

    @Test
    void outputPortsRemainFrameworkFreeAndObservationOwned()
            throws Exception {

        Path outputPorts =
                APPLICATION_ROOT.resolve("port/output");

        try (var paths = Files.walk(outputPorts)) {
            var violations = paths
                    .filter(Files::isRegularFile)
                    .filter(path ->
                            path.toString().endsWith(".java")
                    )
                    .flatMap(path -> {
                        try {
                            String source = Files.readString(path);

                            return List.of(
                                            "import com.sixpay.payment.",
                                            "import com.sixpay.customer.verification.",
                                            "import com.sixpay.customer.observation.infrastructure.",
                                            "import com.sixpay.customer.observation.configuration.",

                                            "import org.springframework.",
                                            "import jakarta.persistence.",
                                            "import org.hibernate.",

                                            "RestClient",
                                            "WebClient",
                                            "HttpClient",
                                            "EntityManager",
                                            "JdbcTemplate",

                                            "@Entity",
                                            "@Repository",
                                            "@Service",
                                            "@Component",
                                            "@Transactional"
                                    )
                                    .stream()
                                    .filter(source::contains)
                                    .map(token ->
                                            path
                                                    + " contains forbidden token "
                                                    + token
                                    );
                        } catch (Exception exception) {
                            throw new IllegalStateException(
                                    "Cannot inspect " + path,
                                    exception
                            );
                        }
                    })
                    .toList();

            assertTrue(
                    violations.isEmpty(),
                    () -> "Observation output-port violations: "
                            + violations
            );
        }
    }

    @Test
    void projectionServiceDoesNotObtainTimeOrGenerateIdentifiers()
            throws Exception {

        String source = Files.readString(
                PROJECTION_SERVICE
        );

        for (String forbidden : List.of(
                "Instant.now(",
                "LocalDate.now(",
                "LocalDateTime.now(",
                "OffsetDateTime.now(",
                "ZonedDateTime.now(",
                "System.currentTimeMillis(",
                "System.nanoTime(",
                "UUID.randomUUID("
        )) {
            assertFalse(
                    source.contains(forbidden),
                    () -> "Projection service obtains technical value: "
                            + forbidden
            );
        }

        assertTrue(
                source.contains("idGenerator.nextId()"),
                "Projection service must delegate ID generation"
        );

        assertTrue(
                source.contains("command.observedAt()"),
                "Projection service must use explicit command time"
        );
    }

    @Test
    void projectionServiceDoesNotPersistThroughInfrastructureDirectly()
            throws Exception {

        String source = Files.readString(
                PROJECTION_SERVICE
        );

        for (String forbidden : List.of(
                "import com.sixpay.customer.observation.infrastructure.",
                "JpaRepository",
                "CrudRepository",
                "EntityManager",
                "JdbcTemplate",
                "saveAndFlush(",
                "TransactionTemplate"
        )) {
            assertFalse(
                    source.contains(forbidden),
                    () -> "Direct persistence dependency found: "
                            + forbidden
            );
        }

        assertTrue(
                source.contains(
                        "ObservedCustomerRepository customerRepository"
                )
        );

        assertTrue(
                source.contains(
                        "ObservedPaymentRepository paymentRepository"
                )
        );
    }
}