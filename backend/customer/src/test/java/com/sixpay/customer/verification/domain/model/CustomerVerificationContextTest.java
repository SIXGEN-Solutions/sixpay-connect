package com.sixpay.customer.verification.domain.model;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.customer.verification.domain.exception.CustomerVerificationDomainException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomerVerificationContextTest {

    @Test
    void carriesExistingCorrelationAndOptionalCausationIds() {
        CorrelationId correlationId =
                CorrelationId.of("corr-123");
        UUID causationId = UUID.fromString(
                "c74e165f-df46-463e-a520-188e6df3e5ae"
        );

        CustomerVerificationContext context =
                CustomerVerificationContext.of(
                        correlationId,
                        causationId
                );

        assertEquals(correlationId, context.correlationId());
        assertTrue(context.causationIdOptional().isPresent());
        assertEquals(
                causationId,
                context.causationIdOptional().orElseThrow()
        );
    }

    @Test
    void allowsAbsentCausationId() {
        CustomerVerificationContext context =
                CustomerVerificationContext.of(
                        CorrelationId.of("corr-123"),
                        null
                );

        assertFalse(context.causationIdOptional().isPresent());
    }

    @Test
    void rejectsNilCausationId() {
        assertThrows(
                CustomerVerificationDomainException.class,
                () -> CustomerVerificationContext.of(
                        CorrelationId.of("corr-123"),
                        new UUID(0L, 0L)
                )
        );
    }
}
