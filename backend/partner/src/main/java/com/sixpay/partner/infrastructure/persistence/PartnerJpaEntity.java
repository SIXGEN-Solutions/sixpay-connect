package com.sixpay.partner.infrastructure.persistence;

import com.sixpay.partner.domain.model.Partner;
import com.sixpay.partner.domain.model.PartnerStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "partners")
public class PartnerJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "legal_name", nullable = false, length = 200)
    private String legalName;

    @Column(name = "technical_contact_name", nullable = false, length = 150)
    private String technicalContactName;

    @Column(name = "technical_contact_email", nullable = false, length = 254)
    private String technicalContactEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private PartnerStatus status;

    @Column(name = "status_reason", length = 500)
    private String statusReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @ElementCollection
    @CollectionTable(
            name = "partner_authorized_perimeters",
            joinColumns = @JoinColumn(name = "partner_id")
    )
    @Column(name = "transaction_type", nullable = false, length = 64)
    private Set<String> authorizedTransactionTypes = new LinkedHashSet<>();

    @ElementCollection
    @CollectionTable(
            name = "partner_validation_thresholds",
            joinColumns = @JoinColumn(name = "partner_id")
    )
    private Set<PartnerThresholdEmbeddable> validationThresholds = new LinkedHashSet<>();

    protected PartnerJpaEntity() {
    }

    public static PartnerJpaEntity create(Partner partner) {
        var entity = new PartnerJpaEntity();
        entity.id = partner.id().value();
        entity.createdAt = partner.createdAt();
        entity.synchronize(partner);
        return entity;
    }

    public void synchronize(Partner partner) {
        legalName = partner.legalName().value();
        technicalContactName = partner.technicalContact().name();
        technicalContactEmail = partner.technicalContact().email();
        status = partner.status();
        statusReason = partner.statusReason().orElse(null);
        updatedAt = partner.updatedAt();
        authorizedTransactionTypes.clear();
        authorizedTransactionTypes.addAll(partner.authorizedPerimeter().transactionTypes());
        validationThresholds.clear();
        partner.validationThresholds().stream()
                .map(threshold -> new PartnerThresholdEmbeddable(
                        threshold.transactionType(),
                        threshold.currency(),
                        threshold.amount(),
                        threshold.validationLevels()))
                .forEach(validationThresholds::add);
    }

    public UUID id() {
        return id;
    }

    public String legalName() {
        return legalName;
    }

    public String technicalContactName() {
        return technicalContactName;
    }

    public String technicalContactEmail() {
        return technicalContactEmail;
    }

    public PartnerStatus status() {
        return status;
    }

    public String statusReason() {
        return statusReason;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public Set<String> authorizedTransactionTypes() {
        return Set.copyOf(authorizedTransactionTypes);
    }

    public Set<PartnerThresholdEmbeddable> validationThresholds() {
        return Set.copyOf(validationThresholds);
    }
}
