package com.sixpay.customer.observation.application.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservedCustomerJpaQueryArchitectureTest {

    private static final Path QUERY_ROOT = Path.of(
            "src/main/java/com/sixpay/customer/observation/"
                    + "infrastructure/query"
    );

    @Test
    void customerAdapterUsesProtectedLookupAndKeysetPagination()
            throws Exception {

        String source = Files.readString(
                QUERY_ROOT.resolve(
                        "adapter/JpaObservedCustomerQueryAdapter.java"
                )
        );

        for (String required : List.of(
                "c.niu_search_hash = :niuSearchHash",
                "protector.searchHash(",
                "c.legal_name_search_normalized",
                "financial_institution_code",
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
                    () -> "Missing query implementation: "
                            + required
            );
        }

        for (String forbidden : List.of(
                "OFFSET ",
                "setFirstResult(",
                "COUNT(*)",
                "protector.reveal(criteria.normalizedNiu",
                "SELECT account_binding_fingerprint",
                "a.account_binding_fingerprint"
        )) {
            assertFalse(
                    source.contains(forbidden),
                    () -> "Forbidden query strategy: "
                            + forbidden
            );
        }
    }

    @Test
    void paymentAdapterUsesStableDescendingKeyset()
            throws Exception {

        String source = Files.readString(
                QUERY_ROOT.resolve(
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
                    () -> "Missing payment keyset: "
                            + required
            );
        }

        assertFalse(source.contains("OFFSET "));
        assertFalse(source.contains("COUNT(*)"));
    }

    @Test
    void accountFingerprintNeverCrossesQueryRowsOrViews()
            throws Exception {

        String models =
                Files.readString(
                        QUERY_ROOT.resolve(
                                "model/ObservedInstitutionRow.java"
                        )
                )
                + Files.readString(
                        QUERY_ROOT.resolve(
                                "mapper/"
                                        + "ObservedCustomerQueryRowMapper.java"
                        )
                );

        for (String forbidden : List.of(
                "accountBindingFingerprint",
                "bindingFingerprint",
                "accountNumber",
                "rawAccount"
        )) {
            assertFalse(
                    models.contains(forbidden),
                    () -> "Sensitive account concept exposed: "
                            + forbidden
            );
        }
    }

    @Test
    void infrastructureDependsOnApplicationPortsNotMutationRepository()
            throws Exception {

        String customer = Files.readString(
                QUERY_ROOT.resolve(
                        "adapter/JpaObservedCustomerQueryAdapter.java"
                )
        );

        String payment = Files.readString(
                QUERY_ROOT.resolve(
                        "adapter/"
                                + "JpaObservedCustomerPaymentQueryAdapter.java"
                )
        );

        String combined = customer + payment;

        assertTrue(combined.contains(
                "ObservedCustomerQueryRepository"
        ));
        assertTrue(combined.contains(
                "ObservedCustomerPaymentQueryRepository"
        ));

        assertFalse(combined.contains(
                "application.port.output.ObservedCustomerRepository"
        ));
        assertFalse(combined.contains(
                "ObservedCustomer.reconstitute("
        ));
        assertFalse(combined.contains(
                "ObservedCustomer.observe"
        ));
    }
}
