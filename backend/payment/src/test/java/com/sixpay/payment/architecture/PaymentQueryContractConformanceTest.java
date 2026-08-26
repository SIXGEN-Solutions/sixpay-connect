package com.sixpay.payment.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentQueryContractConformanceTest {

    private static final Path REPOSITORY_ROOT = Path.of("../..");

    @Test
    void paymentQueryUsesPaymentOwnedReadModel() throws Exception {
        String adapter = Files.readString(
                Path.of(
                        "src/main/java/com/sixpay/payment/"
                                + "infrastructure/query/"
                                + "PaymentProjectionReadAdapter.java"
                )
        );

        assertTrue(adapter.contains("payment_observed_customer_link"));
        assertTrue(adapter.contains("NamedParameterJdbcTemplate"));
        assertFalse(adapter.contains("customer_observed_payment"));
        assertFalse(adapter.contains("com.sixpay.customer."));
    }

    @Test
    void observedCustomerFilterIsImplemented() throws Exception {
        String adapter = Files.readString(
                Path.of(
                        "src/main/java/com/sixpay/payment/"
                                + "infrastructure/query/"
                                + "PaymentProjectionReadAdapter.java"
                )
        );

        assertFalse(
                adapter.contains(
                        "|| query.observedCustomerId() != null"
                )
        );

        assertTrue(
                adapter.contains(
                        "\"pocl.observed_customer_id\""
                )
        );
    }

    @Test
    void paymentReadScopeRemainsEnforced() throws Exception {
        String authority = Files.readString(
                Path.of(
                        "src/main/java/com/sixpay/payment/"
                                + "application/security/"
                                + "PaymentAuthority.java"
                )
        );

        String policy = Files.readString(
                Path.of(
                        "src/main/java/com/sixpay/payment/"
                                + "application/security/"
                                + "PaymentAccessPolicy.java"
                )
        );

        assertTrue(
                authority.contains(
                        "READ(\"SCOPE_payment.read\")"
                )
        );

        assertTrue(
                policy.contains(
                        "case SEARCH, READ -> PaymentAuthority.READ"
                )
        );
    }

    @Test
    void contractStillDeclaresObservedCustomerFilter() throws Exception {
        String contract = Files.readString(
                REPOSITORY_ROOT.resolve(
                        "documentation/contracts/internal/"
                                + "payment-query-api-v1.yaml"
                )
        );

        assertTrue(
                contract.contains(
                        "- name: observedCustomerId"
                )
        );
    }
}