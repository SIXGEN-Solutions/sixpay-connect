package com.sixpay.customer.verification.domain.model;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.customer.verification.domain.exception.CustomerVerificationDomainException;
import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Safe request metadata required to correlate a Customer Verification.
 *
 * <p>This context contains no HTTP headers, JWT, API key, credential or
 * transport-specific object.</p>
 *
 * @param correlationId cross-component correlation identifier
 * @param causationId optional identifier of the event or operation that caused
 *                    this verification request
 */
public record CustomerVerificationContext(
        CorrelationId correlationId,
        UUID causationId
) implements ValueObject {

    private static final UUID NIL_UUID = new UUID(0L, 0L);

    public CustomerVerificationContext {
        correlationId = Objects.requireNonNull(
                correlationId,
                "correlationId is required"
        );
        if (causationId != null && NIL_UUID.equals(causationId)) {
            throw new CustomerVerificationDomainException(
                    "Causation ID must not be the nil UUID"
            );
        }
    }

    public static CustomerVerificationContext of(
            CorrelationId correlationId,
            UUID causationId
    ) {
        return new CustomerVerificationContext(
                correlationId,
                causationId
        );
    }

    public Optional<UUID> causationIdOptional() {
        return Optional.ofNullable(causationId);
    }
}
