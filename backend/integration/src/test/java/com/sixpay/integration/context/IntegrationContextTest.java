package com.sixpay.integration.context;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.common.identifier.IdentifierGenerator;
import com.sixpay.common.time.TimeProvider;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IntegrationContextTest {

    @Test
    void shouldCreateIntegrationContext() {
        UUID requestId =
                UUID.fromString(
                        "123e4567-e89b-12d3-a456-426614174000"
                );

        Instant initiatedAt =
                Instant.parse("2026-07-26T12:00:00Z");

        IdentifierGenerator<UUID> identifierGenerator =
                () -> requestId;

        TimeProvider timeProvider = () -> initiatedAt;

        IntegrationContext context =
                IntegrationContext.create(
                        CorrelationId.of("correlation-123"),
                        identifierGenerator,
                        timeProvider
                );

        assertEquals(requestId, context.requestId());

        assertEquals(
                "correlation-123",
                context.correlationId().value()
        );

        assertEquals(
                initiatedAt,
                context.initiatedAt()
        );
    }
}