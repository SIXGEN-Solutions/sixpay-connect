package com.sixpay.payment.application;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentApplicationLayerArchitectureTest {

    private static final Path APPLICATION_ROOT =
            Path.of(
                    "src/main/java/com/sixpay/payment/application"
            );

    @Test
    void expectedApplicationPackagesExist() {
        Set<String> expected = Set.of(
                "command",
                "query",
                "view",
                "port"
        );

        for (String packageName : expected) {
            assertTrue(
                    Files.isDirectory(
                            APPLICATION_ROOT.resolve(packageName)
                    ),
                    () -> "Missing application package: "
                            + packageName
            );
        }
    }

    @Test
    void applicationLayerContainsNoControllerOrInfrastructureDependency()
            throws IOException {

        List<String> forbidden = List.of(
                "@RestController",
                "@Controller",
                "jakarta.persistence",
                "org.hibernate",
                "com.sixpay.payment.infrastructure",
                "KafkaTemplate",
                "RestClient",
                "WebClient"
        );

        try (Stream<Path> paths = Files.walk(APPLICATION_ROOT)) {
            List<String> violations = paths
                    .filter(Files::isRegularFile)
                    .filter(path ->
                            path.toString().endsWith(".java")
                    )
                    .flatMap(path -> {
                        try {
                            String source = Files.readString(path);

                            return forbidden.stream()
                                    .filter(source::contains)
                                    .map(token ->
                                            path + " contains " + token
                                    );
                        } catch (IOException exception) {
                            throw new IllegalStateException(exception);
                        }
                    })
                    .toList();

            assertEquals(List.of(), violations);
        }
    }

    @Test
    void commandsAndQueriesAreImmutableValueTypes()
            throws IOException {

        for (String packageName : List.of("command", "query")) {
            Path root = APPLICATION_ROOT.resolve(packageName);

            try (Stream<Path> paths = Files.list(root)) {
                List<Path> violations = paths
                        .filter(Files::isRegularFile)
                        .filter(path ->
                                path.toString().endsWith(".java")
                        )
                        .filter(path ->
                                !path.getFileName()
                                        .toString()
                                        .equals("package-info.java")
                        )
                        .filter(path -> {
                            try {
                                String source =
                                        Files.readString(path);

                                return !source.contains(" record ")
                                        && !source.contains(" enum ");

                            } catch (IOException exception) {
                                throw new IllegalStateException(
                                        exception
                                );
                            }
                        })
                        .toList();

                assertEquals(
                        List.of(),
                        violations,
                        packageName
                                + " types must be immutable "
                                + "records or enums"
                );
            }
        }
    }

    @Test
    void noControllerExistsInApplicationLayer()
            throws IOException {

        try (Stream<Path> paths = Files.walk(APPLICATION_ROOT)) {
            List<Path> controllers = paths
                    .filter(Files::isRegularFile)
                    .filter(path ->
                            path.getFileName()
                                    .toString()
                                    .endsWith("Controller.java")
                    )
                    .toList();

            assertTrue(controllers.isEmpty());
        }
    }

    @Test
    void expectedOutboundPortsExistAndRemainPaymentOwned()
            throws IOException {

        Path output = APPLICATION_ROOT.resolve("port/output");

        Set<String> requiredFiles = Set.of(
                "CustomerVerificationPort.java",
                "CustomerVerificationRequest.java",
                "CustomerVerificationResponse.java",
                "PaymentAtomicPersistencePort.java",
                "PaymentLookupPort.java",
                "package-info.java"
        );

        try (Stream<Path> paths = Files.list(output)) {
            Set<String> actualFiles = paths
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .collect(Collectors.toSet());

            assertTrue(
                    actualFiles.containsAll(requiredFiles),
                    () -> "Missing expected output-port files. "
                            + "Expected at least: "
                            + requiredFiles
                            + ", actual: "
                            + actualFiles
            );
        }

        try (Stream<Path> paths = Files.list(output)) {
            List<String> violations = paths
                    .filter(Files::isRegularFile)
                    .filter(path ->
                            path.toString().endsWith(".java")
                    )
                    .flatMap(path -> {
                        try {
                            String source = Files.readString(path);

                            return List.of(
                                            "import com.sixpay.customer.",
                                            "import com.sixpay.payment.infrastructure.",
                                            "import org.springframework.",
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
                                    )
                                    .stream()
                                    .filter(source::contains)
                                    .map(token ->
                                            path
                                                    + " contains forbidden token "
                                                    + token
                                    );
                        } catch (IOException exception) {
                            throw new IllegalStateException(
                                    "Cannot inspect " + path,
                                    exception
                            );
                        }
                    })
                    .toList();

            assertTrue(
                    violations.isEmpty(),
                    () -> "Output-port boundary violations: "
                            + violations
            );
        }
    }

    @Test
    void applicationPackageDocumentationNoLongerClaimsSpringAssembly()
            throws IOException {

        String source = Files.readString(
                APPLICATION_ROOT.resolve("package-info.java")
        );

        assertFalse(
                source.contains("Spring assembly boundary")
        );
    }
}