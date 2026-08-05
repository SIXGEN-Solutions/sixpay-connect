package com.sixpay.customer.observation.infrastructure.audit.mapper;

import com.sixpay.customer.observation.application.audit
        .ObservedCustomerAuditAction;
import com.sixpay.customer.observation.application.audit
        .ObservedCustomerAuditContext;
import com.sixpay.customer.observation.application.audit
        .ObservedCustomerAuditOutcome;
import com.sixpay.customer.observation.application.audit
        .ObservedCustomerAuditRecord;
import com.sixpay.customer.observation.domain.model
        .ObservedCustomerId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ObservedCustomerAuditPersistenceMapperTest {

    @Test
    void mapsAllTechnicalFieldsAndCurrentVersion() {
        UUID auditId = UUID.fromString(
                "11111111-1111-4111-8111-111111111111"
        );
        UUID sourceEventId = UUID.fromString(
                "22222222-2222-4222-8222-222222222222"
        );
        UUID paymentId = UUID.fromString(
                "33333333-3333-4333-8333-333333333333"
        );
        ObservedCustomerId customerId =
                ObservedCustomerId.of(
                        UUID.fromString(
                                "44444444-4444-4444-8444-444444444444"
                        )
                );
        Instant occurredAt =
                Instant.parse("2026-08-05T15:00:00Z");

        ObservedCustomerAuditRecord record =
                ObservedCustomerAuditRecord.projection(
                        auditId,
                        ObservedCustomerAuditAction
                                .PROJECTION_APPLIED,
                        ObservedCustomerAuditOutcome.SUCCEEDED,
                        customerId,
                        sourceEventId,
                        paymentId,
                        new ObservedCustomerAuditContext(
                                "sixpay-system",
                                "55555555-5555-4555-8555-555555555555"
                        ),
                        occurredAt,
                        null
                );

        var entity =
                new ObservedCustomerAuditPersistenceMapper()
                        .toEntity(record);

        assertEquals(auditId, entity.getAuditId());
        assertEquals(
                ObservedCustomerAuditAction.PROJECTION_APPLIED,
                entity.getAction()
        );
        assertEquals(
                ObservedCustomerAuditOutcome.SUCCEEDED,
                entity.getOutcome()
        );
        assertEquals(
                customerId.value(),
                entity.getObservedCustomerId()
        );
        assertEquals(paymentId, entity.getPaymentId());
        assertEquals(sourceEventId, entity.getSourceEventId());
        assertEquals("sixpay-system", entity.getActorId());
        assertEquals(
                "55555555-5555-4555-8555-555555555555",
                entity.getCorrelationId()
        );
        assertEquals(occurredAt, entity.getOccurredAt());
        assertEquals(
                ObservedCustomerAuditPersistenceMapper
                        .CURRENT_AUDIT_VERSION,
                entity.getAuditVersion()
        );
        assertNull(entity.getReasonCode());
    }
}
