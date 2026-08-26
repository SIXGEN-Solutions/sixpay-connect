package com.sixpay.customer.management.infrastructure.persistence;

import com.sixpay.customer.management.domain.model.ObservedCustomerLink;
import com.sixpay.customer.management.domain.model.ObservedCustomerLinkStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "customer_observed_master_link")
public class ObservedCustomerLinkJpaEntity {

    @Id
    @Column(
            name = "observed_customer_id",
            nullable = false,
            updatable = false
    )
    private UUID observedCustomerId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "link_status", nullable = false, length = 16)
    private ObservedCustomerLinkStatus status;

    @Column(name = "linked_by", nullable = false, length = 200)
    private String linkedBy;

    @Column(
            name = "link_correlation_id",
            nullable = false,
            length = 150
    )
    private String linkCorrelationId;

    @Column(name = "link_reason", nullable = false, length = 500)
    private String linkReason;

    @Column(name = "linked_at", nullable = false)
    private Instant linkedAt;

    @Column(name = "unlinked_by", length = 200)
    private String unlinkedBy;

    @Column(name = "unlink_correlation_id", length = 150)
    private String unlinkCorrelationId;

    @Column(name = "unlink_reason", length = 500)
    private String unlinkReason;

    @Column(name = "unlinked_at")
    private Instant unlinkedAt;

    @Version
    @Column(name = "row_version", nullable = false)
    private long version;

    protected ObservedCustomerLinkJpaEntity() {
    }

    static ObservedCustomerLinkJpaEntity create(
            ObservedCustomerLink link
    ) {
        ObservedCustomerLinkJpaEntity entity =
                new ObservedCustomerLinkJpaEntity();
        entity.observedCustomerId =
                link.observedCustomerId();
        entity.synchronize(link);
        return entity;
    }

    void synchronize(ObservedCustomerLink link) {
        customerId = link.customerId().value();
        status = link.status();
        linkedBy = link.linkedBy();
        linkCorrelationId = link.linkCorrelationId();
        linkReason = link.linkReason();
        linkedAt = link.linkedAt();
        unlinkedBy = link.unlinkedBy().orElse(null);
        unlinkCorrelationId =
                link.unlinkCorrelationId().orElse(null);
        unlinkReason = link.unlinkReason().orElse(null);
        unlinkedAt = link.unlinkedAt().orElse(null);
    }

    UUID observedCustomerId() { return observedCustomerId; }
    UUID customerId() { return customerId; }
    ObservedCustomerLinkStatus status() { return status; }
    String linkedBy() { return linkedBy; }
    String linkCorrelationId() { return linkCorrelationId; }
    String linkReason() { return linkReason; }
    Instant linkedAt() { return linkedAt; }
    String unlinkedBy() { return unlinkedBy; }
    String unlinkCorrelationId() { return unlinkCorrelationId; }
    String unlinkReason() { return unlinkReason; }
    Instant unlinkedAt() { return unlinkedAt; }
}
