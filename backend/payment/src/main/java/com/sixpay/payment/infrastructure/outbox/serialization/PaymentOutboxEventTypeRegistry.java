package com.sixpay.payment.infrastructure.outbox.serialization;

import com.sixpay.payment.application.event.projection
        .ObservedCustomerProjectionEvent;

import java.util.Map;
import java.util.Objects;

/**
 * Registry of stable Payment outbox contracts.
 *
 * <p>Only logical identifiers and schema versions cross the persistence
 * boundary. Java package and class names are deliberately excluded.</p>
 */
public final class PaymentOutboxEventTypeRegistry {

    public static final String OBSERVED_CUSTOMER_PROJECTION_TYPE =
            "payment.observation-projection";

    private static final Map<ContractKey, ContractDescriptor>
            CONTRACTS = Map.of(
                    new ContractKey(
                            OBSERVED_CUSTOMER_PROJECTION_TYPE,
                            ObservedCustomerProjectionEvent
                                    .CURRENT_EVENT_VERSION
                    ),
                    new ContractDescriptor(
                            OBSERVED_CUSTOMER_PROJECTION_TYPE,
                            ObservedCustomerProjectionEvent
                                    .CURRENT_EVENT_VERSION
                    )
            );

    public ContractDescriptor requireSupported(
            String eventType,
            int eventVersion
    ) {
        String normalizedType = normalizeType(eventType);

        ContractDescriptor descriptor = CONTRACTS.get(
                new ContractKey(
                        normalizedType,
                        eventVersion
                )
        );

        if (descriptor != null) {
            return descriptor;
        }

        boolean knownType = CONTRACTS.keySet()
                .stream()
                .anyMatch(key ->
                        key.eventType().equals(normalizedType)
                );

        if (knownType) {
            throw new UnsupportedPaymentOutboxEventVersionException(
                    normalizedType,
                    eventVersion
            );
        }

        throw new UnknownPaymentOutboxEventTypeException(
                normalizedType
        );
    }

    public ContractDescriptor observedCustomerProjection() {
        return requireSupported(
                OBSERVED_CUSTOMER_PROJECTION_TYPE,
                ObservedCustomerProjectionEvent
                        .CURRENT_EVENT_VERSION
        );
    }

    private static String normalizeType(String eventType) {
        Objects.requireNonNull(
                eventType,
                "eventType is required"
        );

        String normalized = eventType.strip();

        if (normalized.isEmpty()) {
            throw new UnknownPaymentOutboxEventTypeException(
                    normalized
            );
        }

        if (normalized.contains(".class")
                || normalized.startsWith("com.sixpay.")) {
            throw new UnknownPaymentOutboxEventTypeException(
                    normalized
            );
        }

        return normalized;
    }

    public record ContractDescriptor(
            String eventType,
            int eventVersion
    ) {
        public ContractDescriptor {
            Objects.requireNonNull(
                    eventType,
                    "eventType is required"
            );
            if (eventVersion < 1) {
                throw new IllegalArgumentException(
                        "eventVersion must be at least one"
                );
            }
        }
    }

    private record ContractKey(
            String eventType,
            int eventVersion
    ) {
    }
}
