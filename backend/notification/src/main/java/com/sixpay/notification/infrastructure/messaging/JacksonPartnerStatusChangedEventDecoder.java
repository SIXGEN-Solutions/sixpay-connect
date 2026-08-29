package com.sixpay.notification.infrastructure.messaging;

import com.sixpay.notification.application.model.PartnerStatusChangedEvent;
import com.sixpay.notification.application.port.output.PartnerStatusChangedEventDecoder;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.Objects;

public final class JacksonPartnerStatusChangedEventDecoder
        implements PartnerStatusChangedEventDecoder {

    private final ObjectMapper objectMapper;

    public JacksonPartnerStatusChangedEventDecoder(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @Override
    public PartnerStatusChangedEvent decode(String payload) {
        try {
            return objectMapper.readValue(
                    payload,
                    PartnerStatusChangedEvent.class
            );
        } catch (JacksonException exception) {
            throw new IllegalArgumentException(
                    "invalid PartnerStatusChangedIntegrationEvent payload",
                    exception
            );
        }
    }
}
