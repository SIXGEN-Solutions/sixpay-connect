package com.sixpay.customer.observation.application.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservedCustomerQueryApplicationArchitectureTest {

    private static final Path APPLICATION_ROOT = Path.of(
            "src/main/java/com/sixpay/customer/observation/application"
    );

    private static final Path INPUT_ROOT =
            APPLICATION_ROOT.resolve("port/input/query");

    private static final Path OUTPUT_ROOT =
            APPLICATION_ROOT.resolve("port/output/query");

    private static final Path SERVICE =
            APPLICATION_ROOT.resolve(
                    "service/query/ObservedCustomerQueryService.java"
            );

    @Test
    void inputPortsContainOnlyTheApprovedQueryUseCases()
            throws Exception {

        Set<String> expected = Set.of(
                "SearchObservedCustomersUseCase.java",
                "GetObservedCustomerUseCase.java",
                "ListObservedCustomerPaymentsUseCase.java",
                "package-info.java"
        );

        assertEquals(
                expected,
                javaFiles(INPUT_ROOT)
        );
    }

    @Test
    void queryApplicationRemainsFrameworkAndExternalDomainFree()
            throws Exception {

        List<String> forbidden = List.of(
                "import org.springframework.",
                "import jakarta.persistence.",
                "import org.hibernate.",
                "import com.sixpay.payment.",
                "import com.sixpay.customer.observation.api.",
                "import com.sixpay.customer.observation.infrastructure.",
                "RestClient",
                "WebClient",
                "HttpClient",
                "EntityManager",
                "JdbcTemplate",
                "@Entity",
                "@Repository",
                "@Service",
                "@Component",
                "@Transactional",
                "Instant.now(",
                "UUID.randomUUID("
        );

        for (Path root : List.of(
                INPUT_ROOT,
                OUTPUT_ROOT,
                APPLICATION_ROOT.resolve("service/query")
        )) {
            assertNoTokens(
                    root,
                    forbidden
            );
        }
    }

    @Test
    void serviceImplementsAllThreeUseCasesAndOnlyUsesReadPorts()
            throws Exception {

        String source = normalizedSource(SERVICE);

        for (String required : List.of(
                "implementsSearchObservedCustomersUseCase,"
                        + "GetObservedCustomerUseCase,"
                        + "ListObservedCustomerPaymentsUseCase",
                "ObservedCustomerQueryRepository",
                "ObservedCustomerPaymentQueryRepository",
                "ObservedCustomerCursorCodec",
                "cursorCodec.resolveSearch(",
                "cursorCodec.resolvePayments(",
                "customerQueries.findDetailById(",
                "customerQueries.existsById(",
                "paymentQueries.findByCustomer("
        )) {
            assertTrue(
                    source.contains(required),
                    () -> "Missing query orchestration: "
                            + required
            );
        }

        for (String forbidden : List.of(
                "ObservedCustomerRepository",
                "ObservedCustomer.reconstitute(",
                "ObservedCustomer.observeFirst(",
                "customerRepository.save(",
                "paymentRepository.save(",
                "EntityManager",
                "JdbcTemplate"
        )) {
            assertFalse(
                    source.contains(forbidden),
                    () -> "Query service uses mutation or "
                            + "persistence concept: "
                            + forbidden
            );
        }
    }

    @Test
    void serviceValidatesCursorSnapshotAndPageSize()
            throws Exception {

        String source = normalizedSource(SERVICE);

        for (String required : List.of(
                "validateSearchQuery(canonical)",
                "validatePaymentQuery(canonical)",
                "query.snapshotAt().equals(page.snapshotAt())",
                "page.size()>query.size()",
                "SearchObservedCustomersQuery.MAX_SIZE",
                "ListObservedCustomerPaymentsQuery.MAX_SIZE"
        )) {
            assertTrue(
                    source.contains(required),
                    () -> "Missing query validation: "
                            + required
            );
        }
    }

    @Test
    void serviceNeverLoadsOrReconstitutesObservedCustomerAggregate()
            throws Exception {

        String source = normalizedSource(SERVICE);

        for (String forbidden : List.of(
                "ObservedCustomercustomer",
                "ObservedCustomer.reconstitute(",
                "ObservedCustomer.observeFirst(",
                "customerQueries.findByNormalizedNiu(",
                "customerRepository",
                "paymentRepository",
                ".save("
        )) {
            assertFalse(
                    source.contains(forbidden),
                    () -> "Query service loads or mutates aggregate "
                            + "through: "
                            + forbidden
            );
        }
    }

    @Test
    void notFoundAndUnavailableAreDistinctApplicationExceptions()
            throws Exception {

        String notFound = normalizedSource(
                APPLICATION_ROOT.resolve(
                        "exception/"
                                + "ObservedCustomerNotFoundException.java"
                )
        );

        String unavailable = normalizedSource(
                APPLICATION_ROOT.resolve(
                        "exception/"
                                + "ObservedCustomerQueryUnavailableException.java"
                )
        );

        assertTrue(
                notFound.contains(
                        "extendsRuntimeException"
                )
        );

        assertTrue(
                unavailable.contains(
                        "extendsRuntimeException"
                )
        );

        assertFalse(
                notFound.contains(
                        "ObservedCustomerQueryUnavailableException"
                )
        );

        assertTrue(
                normalizedSource(SERVICE).contains(
                        "newObservedCustomerNotFoundException("
                )
        );

        assertTrue(
                normalizedSource(SERVICE).contains(
                        "newObservedCustomerQueryUnavailableException("
                )
        );
    }

    @Test
    void outputPortsRemainCustomerOwnedAndProjectionFocused()
            throws Exception {

        String customerRepository = normalizedSource(
                OUTPUT_ROOT.resolve(
                        "ObservedCustomerQueryRepository.java"
                )
        );

        String paymentRepository = normalizedSource(
                OUTPUT_ROOT.resolve(
                        "ObservedCustomerPaymentQueryRepository.java"
                )
        );

        String cursorCodec = normalizedSource(
                OUTPUT_ROOT.resolve(
                        "ObservedCustomerCursorCodec.java"
                )
        );

        assertTrue(
                customerRepository.contains(
                        "ObservedCustomerSearchPagesearch("
                )
        );

        assertTrue(
                customerRepository.contains(
                        "Optional<ObservedCustomerDetailView>"
                                + "findDetailById("
                )
        );

        assertTrue(
                customerRepository.contains(
                        "booleanexistsById("
                )
        );

        assertTrue(
                paymentRepository.contains(
                        "ObservedCustomerPaymentPagefindByCustomer("
                )
        );

        assertTrue(
                cursorCodec.contains(
                        "SearchObservedCustomersQueryresolveSearch("
                )
        );

        assertTrue(
                cursorCodec.contains(
                        "ListObservedCustomerPaymentsQuery"
                                + "resolvePayments("
                )
        );
    }

    private static Set<String> javaFiles(
            Path root
    ) throws Exception {

        try (Stream<Path> paths = Files.list(root)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path ->
                            path.toString().endsWith(".java")
                    )
                    .map(path ->
                            path.getFileName().toString()
                    )
                    .collect(Collectors.toSet());
        }
    }

    private static void assertNoTokens(
            Path root,
            List<String> forbidden
    ) throws Exception {

        if (!Files.isDirectory(root)) {
            return;
        }

        try (Stream<Path> paths = Files.walk(root)) {
            List<String> violations = paths
                    .filter(Files::isRegularFile)
                    .filter(path ->
                            path.toString().endsWith(".java")
                    )
                    .flatMap(path -> {
                        try {
                            String source =
                                    Files.readString(path);

                            return forbidden.stream()
                                    .filter(source::contains)
                                    .map(token ->
                                            path
                                                    + " contains "
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
                    () -> "Observed Customer query application "
                            + "violations: "
                            + violations
            );
        }
    }

    private static String normalizedSource(
            Path path
    ) throws Exception {

        return Files.readString(path)
                .replaceAll("\\s+", "");
    }
}