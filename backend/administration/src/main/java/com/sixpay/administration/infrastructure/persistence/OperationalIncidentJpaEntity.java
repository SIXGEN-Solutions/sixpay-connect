package com.sixpay.administration.infrastructure.persistence;

import com.sixpay.administration.domain.model.IncidentId;
import com.sixpay.administration.domain.model.IncidentSeverity;
import com.sixpay.administration.domain.model.IncidentStatus;
import com.sixpay.administration.domain.model.OperationalIncident;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "operational_incident")
public class OperationalIncidentJpaEntity {

    @Id
    @Column(
            name = "incident_id",
            nullable = false,
            updatable = false,
            length = 64
    )
    private String incidentId;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "severity",
            nullable = false,
            length = 16
    )
    private IncidentSeverity severity;

    @Column(
            name = "component",
            nullable = false,
            length = 128
    )
    private String component;

    @Column(
            name = "summary",
            nullable = false,
            length = 256
    )
    private String summary;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 32
    )
    private IncidentStatus status;

    @Column(
            name = "description",
            nullable = false,
            length = 4096
    )
    private String description;

    @Column(
            name = "impact",
            nullable = false,
            length = 2048
    )
    private String impact;

    @Column(name = "accounting_batch_id")
    private UUID accountingBatchId;

    @Column(name = "payment_id")
    private UUID paymentId;

    @Column(
            name = "payment_reference",
            length = 64
    )
    private String paymentReference;

    @Column(name = "correlation_id")
    private UUID correlationId;

    @Column(
            name = "opened_at",
            nullable = false
    )
    private Instant openedAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private Instant updatedAt;

    @OneToMany(
            mappedBy = "incident",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("sequenceNo ASC")
    private List<IncidentTimelineJpaEntity> timeline =
            new ArrayList<>();

    protected OperationalIncidentJpaEntity() {
    }

    OperationalIncident toDomain() {
        return new OperationalIncident(
                new IncidentId(incidentId),
                severity,
                component,
                summary,
                status,
                description,
                impact,
                accountingBatchId,
                paymentId,
                paymentReference,
                correlationId,
                openedAt,
                updatedAt,
                timeline.stream()
                        .map(
                                IncidentTimelineJpaEntity::toDomain
                        )
                        .toList()
        );
    }
}
