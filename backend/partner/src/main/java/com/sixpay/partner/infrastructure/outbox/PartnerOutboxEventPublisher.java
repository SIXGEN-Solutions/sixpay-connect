package com.sixpay.partner.infrastructure.outbox;

import com.sixpay.common.time.TimeProvider;
import com.sixpay.partner.application.port.output.PartnerEventPublisher;
import com.sixpay.partner.events.PartnerIntegrationEvent;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Repository
public class PartnerOutboxEventPublisher implements PartnerEventPublisher {

    private final OutboxEventSpringDataRepository repository;
    private final ObjectMapper objectMapper;
    private final TimeProvider timeProvider;

    public PartnerOutboxEventPublisher(
            OutboxEventSpringDataRepository repository,
            ObjectMapper objectMapper,
            TimeProvider timeProvider
    ) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.timeProvider = timeProvider;
    }

    @Override
    public void publish(PartnerIntegrationEvent event) {
        try {
            repository.save(new OutboxEventJpaEntity(
                    event.eventId(),
                    event.partnerId(),
                    event.getClass().getSimpleName(),
                    event.schemaVersion(),
                    event.correlationId(),
                    objectMapper.writeValueAsString(event),
                    event.occurredAt(),
                    timeProvider.now()
            ));
        } catch (JacksonException exception) {
            throw new IllegalStateException("partner integration event serialization failed", exception);
        }
    }
}
