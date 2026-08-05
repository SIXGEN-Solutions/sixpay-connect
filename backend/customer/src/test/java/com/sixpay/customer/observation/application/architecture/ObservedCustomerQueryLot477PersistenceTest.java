package com.sixpay.customer.observation.application.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservedCustomerQueryLot477PersistenceTest {

    private static final Path QUERY = Path.of(
            "src/main/java/com/sixpay/customer/observation/"
                    + "infrastructure/query"
    );

    @Test
    void customerAdapterUsesExactHashFiltersAndStableKeyset()
            throws Exception {
        String source = Files.readString(
                QUERY.resolve(
                        "adapter/JpaObservedCustomerQueryAdapter.java"
                )
        );

        for (String required : List.of(
                "c.niu_search_hash = :niuSearchHash",
                "protector.searchHash(",
                "c.legal_name_search_normalized",
                "i.financial_institution_code",
                "c.last_payment_status",
                "c.last_failure_reason_code",
                "c.first_observed_at",
                "c.last_observed_at",
                "p.payment_created_at",
                "c.updated_at <= :snapshotAt",
                "c.observed_customer_id",
                "Math.addExact(criteria.size(), 1)",
                "query.setMaxResults("
        )) {
            assertTrue(
                    source.contains(required),
                    () -> "Missing persistence behavior: " + required
            );
        }

        for (String forbidden : List.of(
                "setFirstResult(",
                " OFFSET ",
                "COUNT(*)",
                "protector.reveal(criteria.normalizedNiu",
                "account_binding_fingerprint"
        )) {
            assertFalse(
                    source.contains(forbidden),
                    () -> "Forbidden persistence strategy: " + forbidden
            );
        }
    }

    @Test
    void paymentAdapterUsesDescendingTieBreakerWithoutDuplicates()
            throws Exception {
        String source = Files.readString(
                QUERY.resolve(
                        "adapter/"
                                + "JpaObservedCustomerPaymentQueryAdapter.java"
                )
        );

        for (String required : List.of(
                "p.payment_updated_at <= :snapshotAt",
                "p.payment_created_at < :lastCreatedAt",
                "p.payment_created_at = :lastCreatedAt",
                "p.payment_id < :lastPaymentId",
                "p.payment_created_at DESC",
                "p.payment_id DESC",
                "Math.addExact(criteria.size(), 1)"
        )) {
            assertTrue(
                    source.contains(required),
                    () -> "Missing payment pagination behavior: "
                            + required
            );
        }

        assertFalse(source.contains("setFirstResult("));
        assertFalse(source.contains("COUNT(*)"));
    }

    @Test
    void detailLoadsInstitutionsAndOnlyMaskedAccounts()
            throws Exception {
        String source = Files.readString(
                QUERY.resolve(
                        "adapter/JpaObservedCustomerQueryAdapter.java"
                )
        );
        String model = Files.readString(
                QUERY.resolve(
                        "model/ObservedInstitutionRow.java"
                )
        );

        assertTrue(source.contains(
                "FROM customer_observed_institution i"
        ));
        assertTrue(source.contains(
                "LEFT JOIN customer_observed_account a"
        ));
        assertTrue(source.contains("a.masked_value"));
        assertTrue(model.contains("String maskedValue"));

        assertFalse(source.contains(
                "a.account_binding_fingerprint"
        ));
        assertFalse(model.contains(
                "accountBindingFingerprint"
        ));
    }

    @Test
    void snapshotBarrierIsAppliedBeforeKeyset()
            throws Exception {
        String customer = Files.readString(
                QUERY.resolve(
                        "adapter/JpaObservedCustomerQueryAdapter.java"
                )
        );
        String payment = Files.readString(
                QUERY.resolve(
                        "adapter/"
                                + "JpaObservedCustomerPaymentQueryAdapter.java"
                )
        );

        assertTrue(
                customer.indexOf("c.updated_at <= :snapshotAt")
                        < customer.indexOf("appendKeyset(")
        );
        assertTrue(payment.contains(
                "p.payment_updated_at <= :snapshotAt"
        ));
    }
}
