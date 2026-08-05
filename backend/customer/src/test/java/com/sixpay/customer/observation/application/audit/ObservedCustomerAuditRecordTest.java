package com.sixpay.customer.observation.application.audit;

import com.sixpay.customer.observation.domain.model.ObservedCustomerId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservedCustomerAuditRecordTest {

    private static final UUID AUDIT_ID = UUID.fromString(
            "11111111-1111-4111-8111-111111111111"
    );

    private static final UUID SOURCE_EVENT_ID = UUID.fromString(
            "22222222-2222-4222-8222-222222222222"
    );

    private static final UUID PAYMENT_ID = UUID.fromString(
            "33333333-3333-4333-8333-333333333333"
    );

    private static final ObservedCustomerId CUSTOMER_ID =
            ObservedCustomerId.of(
                    UUID.fromString(
                            "44444444-4444-4444-8444-444444444444"
                    )
            );

    private static final Instant OCCURRED_AT =
            Instant.parse("2026-08-05T15:00:00Z");

    private static final ObservedCustomerAuditContext CONTEXT =
            new ObservedCustomerAuditContext(
                    "service-account:bootstrap",
                    "55555555-5555-4555-8555-555555555555"
            );

    @Test
    void createsProjectionAuditWithTechnicalReferencesOnly() {
        ObservedCustomerAuditRecord record =
                ObservedCustomerAuditRecord.projection(
                        AUDIT_ID,
                        ObservedCustomerAuditAction
                                .PROJECTION_APPLIED,
                        ObservedCustomerAuditOutcome.SUCCEEDED,
                        CUSTOMER_ID,
                        SOURCE_EVENT_ID,
                        PAYMENT_ID,
                        CONTEXT,
                        OCCURRED_AT,
                        null
                );

        assertEquals(AUDIT_ID, record.auditId());
        assertEquals(CUSTOMER_ID, record.observedCustomerId());
        assertEquals(SOURCE_EVENT_ID, record.sourceEventId());
        assertEquals(PAYMENT_ID, record.paymentId());
        assertEquals(CONTEXT.actorId(), record.actorId());
        assertEquals(
                CONTEXT.correlationId(),
                record.correlationId()
        );
        assertNull(record.reasonCode());
    }

    @Test
    void createsSearchAuditWithoutCustomerOrPaymentReferences() {
        ObservedCustomerAuditRecord record =
                ObservedCustomerAuditRecord.query(
                        AUDIT_ID,
                        ObservedCustomerAuditAction.QUERY_SEARCHED,
                        ObservedCustomerAuditOutcome.SUCCEEDED,
                        null,
                        CONTEXT,
                        OCCURRED_AT,
                        null
                );

        assertNull(record.observedCustomerId());
        assertNull(record.sourceEventId());
        assertNull(record.paymentId());
    }

    @Test
    void detailAndPaymentQueryRequireCustomerId() {
        assertThrows(
                NullPointerException.class,
                () -> ObservedCustomerAuditRecord.query(
                        AUDIT_ID,
                        ObservedCustomerAuditAction
                                .QUERY_DETAIL_READ,
                        ObservedCustomerAuditOutcome.SUCCEEDED,
                        null,
                        CONTEXT,
                        OCCURRED_AT,
                        null
                )
        );

        assertThrows(
                NullPointerException.class,
                () -> ObservedCustomerAuditRecord.query(
                        AUDIT_ID,
                        ObservedCustomerAuditAction
                                .QUERY_PAYMENTS_LISTED,
                        ObservedCustomerAuditOutcome.SUCCEEDED,
                        null,
                        CONTEXT,
                        OCCURRED_AT,
                        null
                )
        );
    }

    @Test
    void projectionRequiresSourceEventAndPaymentIds() {
        assertThrows(
                NullPointerException.class,
                () -> ObservedCustomerAuditRecord.projection(
                        AUDIT_ID,
                        ObservedCustomerAuditAction
                                .PROJECTION_APPLIED,
                        ObservedCustomerAuditOutcome.SUCCEEDED,
                        CUSTOMER_ID,
                        null,
                        PAYMENT_ID,
                        CONTEXT,
                        OCCURRED_AT,
                        null
                )
        );

        assertThrows(
                NullPointerException.class,
                () -> ObservedCustomerAuditRecord.projection(
                        AUDIT_ID,
                        ObservedCustomerAuditAction
                                .PROJECTION_APPLIED,
                        ObservedCustomerAuditOutcome.SUCCEEDED,
                        CUSTOMER_ID,
                        SOURCE_EVENT_ID,
                        null,
                        CONTEXT,
                        OCCURRED_AT,
                        null
                )
        );
    }

    @Test
    void factoriesRejectActionsFromTheWrongFamily() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ObservedCustomerAuditRecord.projection(
                        AUDIT_ID,
                        ObservedCustomerAuditAction.QUERY_SEARCHED,
                        ObservedCustomerAuditOutcome.SUCCEEDED,
                        CUSTOMER_ID,
                        SOURCE_EVENT_ID,
                        PAYMENT_ID,
                        CONTEXT,
                        OCCURRED_AT,
                        null
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> ObservedCustomerAuditRecord.query(
                        AUDIT_ID,
                        ObservedCustomerAuditAction
                                .PROJECTION_REPLAYED,
                        ObservedCustomerAuditOutcome.REPLAYED,
                        CUSTOMER_ID,
                        CONTEXT,
                        OCCURRED_AT,
                        null
                )
        );
    }

    @Test
    void reasonCodeIsBoundedAndMustRemainTechnical() {
        ObservedCustomerAuditRecord record =
                ObservedCustomerAuditRecord.query(
                        AUDIT_ID,
                        ObservedCustomerAuditAction.QUERY_FAILED,
                        ObservedCustomerAuditOutcome.FAILED,
                        CUSTOMER_ID,
                        CONTEXT,
                        OCCURRED_AT,
                        "QUERY_TEMPORARILY_UNAVAILABLE"
                );

        assertEquals(
                "QUERY_TEMPORARILY_UNAVAILABLE",
                record.reasonCode()
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> ObservedCustomerAuditRecord.query(
                        AUDIT_ID,
                        ObservedCustomerAuditAction.QUERY_FAILED,
                        ObservedCustomerAuditOutcome.FAILED,
                        CUSTOMER_ID,
                        CONTEXT,
                        OCCURRED_AT,
                        "database failed for M0123456"
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> ObservedCustomerAuditRecord.query(
                        AUDIT_ID,
                        ObservedCustomerAuditAction.QUERY_FAILED,
                        ObservedCustomerAuditOutcome.FAILED,
                        CUSTOMER_ID,
                        CONTEXT,
                        OCCURRED_AT,
                        "A".repeat(101)
                )
        );
    }

    @Test
    void stringRenderingDoesNotExposeActorCredentialMaterial() {
        ObservedCustomerAuditRecord record =
                ObservedCustomerAuditRecord.query(
                        AUDIT_ID,
                        ObservedCustomerAuditAction.QUERY_DENIED,
                        ObservedCustomerAuditOutcome.DENIED,
                        CUSTOMER_ID,
                        CONTEXT,
                        OCCURRED_AT,
                        "SCOPE_MISSING"
                );

        String rendered = record.toString();

        assertTrue(rendered.contains("actorId=[PROTECTED]"));
        assertFalse(rendered.contains(CONTEXT.actorId()));
    }
}
