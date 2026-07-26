package com.sixpay.integration.context;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.common.identifier.IdentifierGenerator;
import com.sixpay.common.time.TimeProvider;
import com.sixpay.common.validation.Preconditions;

import java.time.Instant;
import java.util.UUID;

/**
 * Carries technical tracing information associated with an
 * external system request.
 *
 * @param requestId unique external request identifier
 * @param correlationId end-to-end correlation identifier
 * @param initiatedAt request creation time
 */
public record IntegrationContext(
        UUID requestId,
        CorrelationId correlationId,
        Instant initiatedAt
) {

    public IntegrationContext {
        requestId = Preconditions.requireNonNull(
                requestId,
                "Integration request ID must not be null"
        );

        correlationId = Preconditions.requireNonNull(
                correlationId,
                "Correlation ID must not be null"
        );

        initiatedAt = Preconditions.requireNonNull(
                initiatedAt,
                "Integration initiation time must not be null"
        );
    }

    public static IntegrationContext create(
            CorrelationId correlationId,
            IdentifierGenerator<UUID> identifierGenerator,
            TimeProvider timeProvider
    ) {
        Preconditions.requireNonNull(
                identifierGenerator,
                "Identifier generator must not be null"
        );

        Preconditions.requireNonNull(
                timeProvider,
                "Time provider must not be null"
        );

        return new IntegrationContext(
                identifierGenerator.generate(),
                correlationId,
                timeProvider.now()
        );
    }
}