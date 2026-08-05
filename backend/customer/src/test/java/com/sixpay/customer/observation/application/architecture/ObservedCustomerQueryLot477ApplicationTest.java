package com.sixpay.customer.observation.application.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservedCustomerQueryLot477ApplicationTest {

    private static final Path APPLICATION = Path.of(
            "src/main/java/com/sixpay/customer/observation/application"
    );

    @Test
    void queryModelEnforcesMaximumPageSizeAndTemporalValidation()
            throws Exception {
        String search = Files.readString(
                APPLICATION.resolve(
                        "query/SearchObservedCustomersQuery.java"
                )
        );
        String payments = Files.readString(
                APPLICATION.resolve(
                        "query/ListObservedCustomerPaymentsQuery.java"
                )
        );
        String service = Files.readString(
                APPLICATION.resolve(
                        "service/query/ObservedCustomerQueryService.java"
                )
        );

        assertTrue(search.contains("MAX_SIZE = 200"));
        assertTrue(payments.contains("MAX_SIZE = 200"));

        for (String required : List.of(
                "firstObservedFrom",
                "firstObservedTo",
                "lastObservedFrom",
                "lastObservedTo",
                "paymentFrom",
                "paymentTo",
                "createdFrom",
                "createdTo"
        )) {
            assertTrue(
                    search.contains(required)
                            || payments.contains(required),
                    () -> "Missing time filter: " + required
            );
        }

        assertTrue(service.contains("decodeSearch("));
        assertTrue(service.contains("decodePayments("));
        assertTrue(service.contains("criteria.snapshotAt()"));
        assertTrue(service.contains("slice.hasMore()"));
        assertTrue(service.contains("slice.nextPosition()"));
    }

    @Test
    void cursorCodecAuthenticatesVersionSortSnapshotAndQuery()
            throws Exception {
        String codec = Files.readString(
                Path.of(
                        "src/main/java/com/sixpay/customer/observation/"
                                + "infrastructure/query/cursor/"
                                + "HmacObservedCustomerCursorCodec.java"
                )
        );

        for (String required : List.of(
                "SCHEMA_VERSION = 1",
                "HmacSHA256",
                "MessageDigest.isEqual(",
                "cursor sort does not match the request",
                "cursor snapshot does not match the request",
                "cursor query does not match the request"
        )) {
            assertTrue(
                    codec.contains(required),
                    () -> "Missing cursor guarantee: " + required
            );
        }
    }

    @Test
    void applicationRemainsFrameworkAndPersistenceFree()
            throws Exception {
        try (var paths = Files.walk(APPLICATION)) {
            List<String> violations = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .flatMap(path -> {
                        try {
                            String source = Files.readString(path);
                            return List.of(
                                            "import org.springframework.",
                                            "import jakarta.persistence.",
                                            "import org.hibernate.",
                                            "EntityManager",
                                            "JdbcTemplate"
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
                    () -> "Application violations: " + violations
            );
        }

        String service = Files.readString(
                APPLICATION.resolve(
                        "service/query/ObservedCustomerQueryService.java"
                )
        );
        assertFalse(service.contains("ObservedCustomer.reconstitute("));
        assertFalse(service.contains("ObservedCustomerRepository"));
    }
}
