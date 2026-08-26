package com.sixpay.customer.observation.contract;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservedCustomerOpenApiContractTest {

    private static final Path CONTRACT = locateContract();

    @Test
    void contractIsVersionedReadOnlyAndExposesExactlyThreeGetRoutes()
            throws Exception {
        String source = Files.readString(CONTRACT);

        for (String required : List.of(
                "openapi: 3.1.0",
                "registryId: observed-customer-query-api-v1",
                "readOnly: true",
                "/internal/api/v1/observed-customers:",
                "/internal/api/v1/observed-customers/{observedCustomerId}:",
                "/internal/api/v1/observed-customers/{observedCustomerId}/payments:",
                "operationId: searchObservedCustomers",
                "operationId: getObservedCustomer",
                "operationId: listObservedCustomerPayments"
        )) {
            assertTrue(
                    source.contains(required),
                    () -> "Missing OpenAPI contract element: " + required
            );
        }

        assertFalse(source.contains("\n    post:"));
        assertFalse(source.contains("\n    put:"));
        assertFalse(source.contains("\n    patch:"));
        assertFalse(source.contains("\n    delete:"));
    }

    @Test
    void securityCorrelationAndPaginationMatchTheInternalContract()
            throws Exception {
        String source = Files.readString(CONTRACT);

        for (String required : List.of(
                "observed-customer.read",
                "name: X-Correlation-ID",
                "required: true",
                "format: uuid",
                "maximum: 200",
                "default: 50",
                "'400':",
                "'401':",
                "'403':",
                "'404':",
                "'429':",
                "'500':",
                "'503':"
        )) {
            assertTrue(
                    source.contains(required),
                    () -> "Missing OpenAPI constraint: " + required
            );
        }
    }

    @Test
    void contractNeverExposesRawBankingOrFingerprintFields()
            throws Exception {
        String source = Files.readString(CONTRACT);

        for (String forbidden : List.of(
                "accountNumber:",
                "rawAccount",
                "accountBindingFingerprint",
                "integrationAccountToken",
                "ribDebiteur",
                "jwt:",
                "apiKey:"
        )) {
            assertFalse(
                    source.contains(forbidden),
                    () -> "Sensitive OpenAPI field: " + forbidden
            );
        }

        assertTrue(source.contains("MaskedAccountReference"));
        assertTrue(source.contains("maskedValue"));
    }

    @Test
    void paymentAmountUsesThePublishedNestedShape()
            throws Exception {
        String source = Files.readString(CONTRACT);

        assertTrue(source.contains(
                "ObservedCustomerPaymentReference:"
        ));
        assertTrue(source.contains(
                "required: [amount, currency]"
        ));
        assertTrue(source.contains(
                "pattern: '^[A-Z]{3}$'"
        ));
    }

    private static Path locateContract() {
        for (Path candidate : List.of(
                Path.of(
                        "../../documentation/contracts/internal/"
                                + "observed-customer-query-api-v1.yaml"
                ),
                Path.of(
                        "documentation/contracts/internal/"
                                + "observed-customer-query-api-v1.yaml"
                )
        )) {
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }

        throw new IllegalStateException(
                "Cannot locate observed-customer-query-api-v1.yaml"
        );
    }
}
