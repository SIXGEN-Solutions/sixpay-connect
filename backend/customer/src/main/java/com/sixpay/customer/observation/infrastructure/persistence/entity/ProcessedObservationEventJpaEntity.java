package com.sixpay.customer.observation.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "customer_observation_processed_event")
public class ProcessedObservationEventJpaEntity {

    @Id
    @Column(name = "source_event_id", nullable = false)
    private UUID sourceEventId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "observed_customer_id", nullable = false)
    private ObservedCustomerJpaEntity observedCustomer;

    @Column(name = "source_event_watermark", nullable = false, length = 256)
    private String sourceEventWatermark;

    @Column(name = "observed_at", nullable = false)
    private Instant observedAt;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    protected ProcessedObservationEventJpaEntity() {
        // Required by JPA.
    }

    public static ProcessedObservationEventJpaEntity create() {
        return new ProcessedObservationEventJpaEntity();
    }

    public UUID getSourceEventId() { return sourceEventId; }
    public void setSourceEventId(UUID value) { this.sourceEventId = value; }
    public ObservedCustomerJpaEntity getObservedCustomer() { return observedCustomer; }
    public void setObservedCustomer(ObservedCustomerJpaEntity value) { this.observedCustomer = value; }
    public String getSourceEventWatermark() { return sourceEventWatermark; }
    public void setSourceEventWatermark(String value) { this.sourceEventWatermark = value; }
    public Instant getObservedAt() { return observedAt; }
    public void setObservedAt(Instant value) { this.observedAt = value; }
    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant value) { this.processedAt = value; }
}
