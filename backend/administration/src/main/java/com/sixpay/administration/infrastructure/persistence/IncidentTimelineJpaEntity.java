package com.sixpay.administration.infrastructure.persistence;

import com.sixpay.administration.domain.model.IncidentTimelineEntry;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "operational_incident_timeline")
public class IncidentTimelineJpaEntity {

    @Id
    @Column(
            name = "event_id",
            nullable = false,
            updatable = false,
            length = 64
    )
    private String eventId;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "incident_id",
            nullable = false
    )
    private OperationalIncidentJpaEntity incident;

    @Column(
            name = "occurred_at",
            nullable = false
    )
    private Instant occurredAt;

    @Column(
            name = "message",
            nullable = false,
            length = 1024
    )
    private String message;

    @Column(
            name = "actor",
            nullable = false,
            length = 128
    )
    private String actor;

    @Column(
            name = "sequence_no",
            nullable = false
    )
    private int sequenceNo;

    protected IncidentTimelineJpaEntity() {
    }

    IncidentTimelineEntry toDomain() {
        return new IncidentTimelineEntry(
                eventId,
                occurredAt,
                message,
                actor,
                sequenceNo
        );
    }
}
