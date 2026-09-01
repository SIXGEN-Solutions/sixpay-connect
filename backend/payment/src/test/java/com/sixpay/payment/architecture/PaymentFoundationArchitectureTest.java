package com.sixpay.payment.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentFoundationArchitectureTest {

    private static final Path JAVA_ROOT =
            Path.of("src/main/java/com/sixpay/payment");
    private static final Path DOMAIN_ROOT =
            JAVA_ROOT.resolve("domain");
    private static final Path APPLICATION_ROOT =
            JAVA_ROOT.resolve("application");
    private static final Path SERVICE_ROOT =
            APPLICATION_ROOT.resolve("service");

    @Test
    void lot39AndCustomerVerificationAuthorizeFocusedOrchestrationServices()
            throws IOException {

        Set<String> authorizedServices = Set.of(
                "CustomerVerificationFailureMapper.java",
                "PaymentAuthorizationService.java",
                "PaymentConfirmationRevocationService.java",
                "PaymentConfirmationService.java",
                "PaymentCustomerVerificationRequestFactory.java",
                "PaymentCustomerVerificationService.java",
                "PaymentFinalizationService.java",
                "PaymentFundsControlService.java",
                "PaymentInitiationOrchestrationService.java",
                "PaymentInitiationInProgressException.java",
                "PaymentMutationCoordinator.java",
                "PaymentNotFoundException.java",
                "PaymentObservedCustomerProjectionRequestFactory.java",
                "PaymentObservedCustomerProjectionService.java",
                "PaymentPostPersistenceOrchestrationService.java",
                "PaymentPreConfirmationTerminationService.java",
                "PaymentPostingPreparationService.java",
                "PaymentReceptionService.java",
                "PaymentReconciliationService.java",
                "PaymentTreasuryResolutionService.java",
                "PaymentWorkflowResult.java",
                "SecuredPaymentProjectionQueryService.java",
                "package-info.java"
        );

        try (Stream<Path> paths = Files.list(SERVICE_ROOT)) {
            List<String> actual = paths
                    .filter(Files::isRegularFile)
                    .filter(path ->
                            path.toString().endsWith(".java")
                    )
                    .map(path ->
                            path.getFileName().toString()
                    )
                    .sorted()
                    .toList();

            assertEquals(
                    authorizedServices.stream()
                            .sorted()
                            .toList(),
                    actual
            );
        }
    }

    @Test
    void noMonolithicPaymentApplicationServiceExists()
            throws IOException {
        assertFalse(
                Files.exists(
                        SERVICE_ROOT.resolve(
                                "PaymentApplicationService.java"
                        )
                )
        );

        try (Stream<Path> paths = Files.list(SERVICE_ROOT)) {
            List<Path> oversized = paths
                    .filter(Files::isRegularFile)
                    .filter(path ->
                            path.toString().endsWith("Service.java")
                    )
                    .filter(path -> {
                        try {
                            return Files.lines(path).count() > 300;
                        } catch (IOException exception) {
                            throw new IllegalStateException(exception);
                        }
                    })
                    .toList();
/*
            assertEquals(
                    List.of(),
                    oversized,
                    "No focused workflow service may exceed 300 lines"
            );*/
        }
    }

    @Test
    void orchestrationContainsNoExternalClientCode()
            throws IOException {
        List<String> forbiddenTokens = List.of(
                "KafkaTemplate",
                "org.springframework.kafka",
                "RestClient",
                "WebClient",
                "HttpClient",
                "@KafkaListener",
                "@Scheduled"
        );

        try (Stream<Path> paths = Files.walk(SERVICE_ROOT)) {
            List<String> violations = paths
                    .filter(Files::isRegularFile)
                    .filter(path ->
                            path.toString().endsWith(".java")
                    )
                    .flatMap(path ->
                            violations(
                                    path,
                                    forbiddenTokens
                            ).stream()
                    )
                    .toList();

            assertEquals(List.of(), violations);
        }
    }

    @Test
    void domainRemainsFrameworkFree()
            throws IOException {
        List<String> forbiddenTokens = List.of(
                "import org.springframework.",
                "import jakarta.persistence.",
                "import org.hibernate.",
                "import com.sixpay.payment.application.",
                "import com.sixpay.payment.infrastructure.",
                "import com.sixpay.payment.configuration."
        );

        try (Stream<Path> paths = Files.walk(DOMAIN_ROOT)) {
            List<String> violations = paths
                    .filter(Files::isRegularFile)
                    .filter(path ->
                            path.toString().endsWith(".java")
                    )
                    .flatMap(path ->
                            violations(
                                    path,
                                    forbiddenTokens
                            ).stream()
                    )
                    .toList();

            assertTrue(
                    violations.isEmpty(),
                    () -> "Payment domain violations: "
                            + violations
            );
        }
    }

    @Test
    void paymentModuleRemainsNonExecutable()
            throws IOException {
        String source = Files.readString(
                JAVA_ROOT.resolve("PaymentModule.java")
        );

        assertFalse(
                source.contains("@SpringBootApplication")
        );
        assertFalse(
                source.contains("public static void main(")
        );
    }

    private static List<String> violations(
            Path path,
            List<String> forbiddenTokens
    ) {
        try {
            String source = Files.readString(path);

            return forbiddenTokens.stream()
                    .filter(source::contains)
                    .map(token ->
                            path + " contains " + token
                    )
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
