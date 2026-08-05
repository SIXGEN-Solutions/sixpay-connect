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

class ObservedCustomerQueryModelArchitectureTest {

    private static final Path ROOT = Path.of(
            "src/main/java/com/sixpay/customer/observation/"
                    + "application/query"
    );

    @Test
    void queryPackageContainsTheApprovedModel()
            throws Exception {

        Set<String> expected = Set.of(
                "SearchObservedCustomersQuery.java",
                "GetObservedCustomerQuery.java",
                "ListObservedCustomerPaymentsQuery.java",
                "ObservedCustomerSort.java",
                "ObservedCustomerCursor.java",
                "ObservedCustomerSearchPage.java",
                "ObservedCustomerSummaryView.java",
                "ObservedCustomerDetailView.java",
                "ObservedCustomerPaymentPage.java",
                "ObservedCustomerPaymentView.java",
                "ObservedInstitutionView.java",
                "ObservedAccountView.java",
                "MaskedIdentifierView.java",

                /*
                 * Lot 4.7.3 — decoded keyset query model.
                 */
                "ObservedCustomerSearchCriteria.java",
                "ObservedCustomerPaymentCriteria.java",
                "ObservedCustomerSearchPosition.java",
                "ObservedCustomerPaymentPosition.java",
                "ObservedCustomerSearchSlice.java",
                "ObservedCustomerPaymentSlice.java",

                "package-info.java"
        );

        try (Stream<Path> paths = Files.list(ROOT)) {
            Set<String> actual = paths
                    .filter(Files::isRegularFile)
                    .filter(path ->
                            path.toString().endsWith(".java")
                    )
                    .map(path ->
                            path.getFileName().toString()
                    )
                    .collect(Collectors.toSet());

            assertEquals(
                    expected,
                    actual
            );
        }
    }

    @Test
    void queryModelIsFrameworkPersistenceAndExternalDomainFree()
            throws Exception {

        List<String> forbidden = List.of(
                "import org.springframework.",
                "import jakarta.persistence.",
                "import jakarta.servlet.",
                "import org.hibernate.",
                "import tools.jackson.",
                "import com.sixpay.payment.",
                "import com.sixpay.customer.observation.api.",
                "import com.sixpay.customer.observation.infrastructure.",
                "RestClient",
                "WebClient",
                "HttpClient",
                "KafkaTemplate",
                "EntityManager",
                "JdbcTemplate",
                "@Entity",
                "@Repository",
                "@Service",
                "@Component",
                "@RestController",
                "@Controller",
                "@JsonProperty",
                "Instant.now(",
                "UUID.randomUUID("
        );

        try (Stream<Path> paths = Files.walk(ROOT)) {
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
                    () -> "Observed Customer query-model "
                            + "violations: "
                            + violations
            );
        }
    }

    @Test
    void queryTypesAreImmutableRecordsOrEnums()
            throws Exception {

        try (Stream<Path> paths = Files.list(ROOT)) {
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
                    () -> "Query types must be records or enums: "
                            + violations
            );
        }
    }

    @Test
    void accountViewsNeverExposeRawAccountOrFingerprint()
            throws Exception {

        String account = Files.readString(
                ROOT.resolve("ObservedAccountView.java")
        );

        String institution = Files.readString(
                ROOT.resolve("ObservedInstitutionView.java")
        );

        String combined = account + institution;

        for (String forbidden : List.of(
                "accountBindingFingerprint",
                "bindingFingerprint",
                "accountNumber",
                "rawAccount",
                "ribDebiteur",
                "integrationAccountToken",
                "DebtorAccountReference"
        )) {
            assertFalse(
                    combined.contains(forbidden),
                    () -> "Forbidden account concept: "
                            + forbidden
            );
        }

        assertTrue(account.contains("String reference"));
        assertTrue(account.contains("String maskedValue"));
    }

    @Test
    void queryContractCarriesStableSnapshotAndBoundedPageSize()
            throws Exception {

        String search = Files.readString(
                ROOT.resolve(
                        "SearchObservedCustomersQuery.java"
                )
        );

        String payments = Files.readString(
                ROOT.resolve(
                        "ListObservedCustomerPaymentsQuery.java"
                )
        );

        String pages = Files.readString(
                ROOT.resolve(
                        "ObservedCustomerSearchPage.java"
                )
        ) + Files.readString(
                ROOT.resolve(
                        "ObservedCustomerPaymentPage.java"
                )
        );

        for (String source : List.of(search, payments)) {
            assertTrue(source.contains("Instant snapshotAt"));
            assertTrue(source.contains("int size"));
            assertTrue(source.contains("MAX_SIZE = 200"));
            assertTrue(source.contains(
                    "snapshotAt is required"
            ));
        }

        assertTrue(pages.contains("boolean hasMore"));
        assertTrue(pages.contains(
                "ObservedCustomerCursor nextCursor"
        ));
        assertTrue(pages.contains("Instant snapshotAt"));
    }

    @Test
    void queryRenderingProtectsPurposeLimitedIdentity()
            throws Exception {

        String search = Files.readString(
                ROOT.resolve(
                        "SearchObservedCustomersQuery.java"
                )
        );

        String summary = Files.readString(
                ROOT.resolve(
                        "ObservedCustomerSummaryView.java"
                )
        );

        String detail = Files.readString(
                ROOT.resolve(
                        "ObservedCustomerDetailView.java"
                )
        );

        assertTrue(search.contains(
                "normalizedNiu=[PROTECTED]"
        ));
        assertTrue(search.contains(
                "legalName=[PROTECTED]"
        ));
        assertTrue(summary.contains(
                "legalName=[PROTECTED]"
        ));
        assertTrue(detail.contains(
                "sourceEventWatermark=[PROTECTED]"
        ));
    }
}
