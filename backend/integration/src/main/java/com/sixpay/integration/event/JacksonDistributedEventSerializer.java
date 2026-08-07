package com.sixpay.integration.event;

import tools.jackson.databind.ObjectMapper;

import java.util.Objects;

public final class JacksonDistributedEventSerializer
        implements DistributedEventSerializer {

    private final ObjectMapper objectMapper;

    public JacksonDistributedEventSerializer(
            ObjectMapper objectMapper
    ) {
        this.objectMapper = Objects.requireNonNull(
                objectMapper
        );
    }

    @Override
    public byte[] serialize(
            DistributedEventEnvelope<?> event
    ) {
        try {
            return objectMapper.writeValueAsBytes(event);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(
                    "Cannot serialize distributed event",
                    exception
            );
        }
    }

    @Override
    public DistributedEventEnvelope<?> deserialize(
            byte[] payload
    ) {
        try {
            return objectMapper.readValue(
                    payload,
                    DistributedEventEnvelope.class
            );
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(
                    "Cannot deserialize distributed event",
                    exception
            );
        }
    }
}
