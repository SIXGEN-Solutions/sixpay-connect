package com.sixpay.customer.observation.application.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservedCustomerCursorArchitectureTest {

    private static final Path APPLICATION = Path.of(
            "src/main/java/com/sixpay/customer/observation/application"
    );

    private static final Path INFRASTRUCTURE = Path.of(
            "src/main/java/com/sixpay/customer/observation/"
                    + "infrastructure/query/cursor"
    );

    @Test
    void readPortsUseCriteriaAndSlicesRatherThanAggregatePages()
            throws Exception {

        String customers = normalized(
                APPLICATION.resolve(
                        "port/output/query/"
                                + "ObservedCustomerQueryRepository.java"
                )
        );

        String payments = normalized(
                APPLICATION.resolve(
                        "port/output/query/"
                                + "ObservedCustomerPaymentQueryRepository.java"
                )
        );

        assertTrue(customers.contains(
                "ObservedCustomerSearchSlicesearch("
        ));
        assertTrue(customers.contains(
                "ObservedCustomerSearchCriteriacriteria"
        ));
        assertTrue(payments.contains(
                "ObservedCustomerPaymentSlicefindByCustomerId("
        ));
        assertTrue(payments.contains(
                "ObservedCustomerPaymentCriteriacriteria"
        ));

        for (String source : List.of(
                customers,
                payments
        )) {
            assertFalse(
                    source.contains(
                            "importcom.sixpay.customer.observation."
                                    + "domain.model.ObservedCustomer;"
                    )
            );

            assertFalse(
                    source.contains(
                            "ObservedCustomerRepository"
                    )
            );

            assertFalse(
                    source.contains(
                            "jakarta.persistence"
                    )
            );

            assertFalse(
                    source.contains(
                            "org.springframework"
                    )
            );
        }
    }

    @Test
    void cursorContractIsVersionedAuthenticatedAndOpaque()
            throws Exception {

        String codec = Files.readString(
                INFRASTRUCTURE.resolve(
                        "HmacObservedCustomerCursorCodec.java"
                )
        );

        for (String required : List.of(
                "SCHEMA_VERSION = 1",
                "HmacSHA256",
                "MessageDigest.isEqual(",
                "Base64.getUrlEncoder()",
                "schema version is not supported",
                "cursor sort does not match the request",
                "cursor query does not match the request"
        )) {
            assertTrue(
                    codec.contains(required),
                    () -> "Missing cursor protection: "
                            + required
            );
        }

        for (String forbidden : List.of(
                "ObjectMapper",
                "JsonMapper",
                "normalizedNiu() +",
                "legalName() +",
                "accountNumber",
                "accountBindingFingerprint",
                "maskedAccountReference"
        )) {
            assertFalse(
                    codec.contains(forbidden),
                    () -> "Cursor leaks or couples to: "
                            + forbidden
            );
        }
    }

    @Test
    void serviceIsTheOnlyApplicationComponentCreatingPages()
            throws Exception {

        String service = normalized(
                APPLICATION.resolve(
                        "service/query/"
                                + "ObservedCustomerQueryService.java"
                )
        );

        assertTrue(service.contains(
                "cursorCodec.decodeSearch(query)"
        ));
        assertTrue(service.contains(
                "cursorCodec.decodePayments(query)"
        ));
        assertTrue(service.contains(
                "customerQueries.search(criteria)"
        ));
        assertTrue(service.contains(
                "paymentQueries.findByCustomerId(criteria)"
        ));
        assertTrue(service.contains(
                "cursorCodec.encodeSearch("
        ));
        assertTrue(service.contains(
                "cursorCodec.encodePayments("
        ));

        assertFalse(service.contains(
                "ObservedCustomer.reconstitute("
        ));
        assertFalse(service.contains(
                "ObservedCustomerRepository"
        ));
    }

    private static String normalized(Path path)
            throws Exception {
        return Files.readString(path)
                .replaceAll("\\s+", "");
    }
}
