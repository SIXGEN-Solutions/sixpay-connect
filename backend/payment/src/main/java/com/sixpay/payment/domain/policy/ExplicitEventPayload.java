package com.sixpay.payment.domain.policy;

import java.util.Map;
import java.util.Objects;

public record ExplicitEventPayload(
        String eventType,
        Map<String, Object> fields
) {
    public ExplicitEventPayload {
        Objects.requireNonNull(eventType, "Event type");
        eventType = eventType.strip();
        if (eventType.isEmpty()) {
            throw new IllegalArgumentException(
                    "Event type must not be blank"
            );
        }
        Objects.requireNonNull(fields, "Event fields");
        fields = Map.copyOf(fields);
    }
}
