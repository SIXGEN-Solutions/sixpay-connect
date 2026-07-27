package com.sixpay.partner.infrastructure.audit;

import com.sixpay.partner.application.port.out.PartnerThresholdHistoryRecord;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "partner_validation_threshold_history")
public class PartnerThresholdHistoryJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "partner_id", nullable = false, updatable = false)
    private UUID partnerId;

    @Column(name = "transaction_type", nullable = false, updatable = false, length = 64)
    private String transactionType;

    @Column(name = "currency", nullable = false, updatable = false, length = 3)
    private String currency;

    @Column(name = "previous_amount", updatable = false, precision = 19, scale = 4)
    private BigDecimal previousAmount;

    @Column(name = "previous_validation_levels", updatable = false)
    private Integer previousValidationLevels;

    @Column(name = "current_amount", nullable = false, updatable = false, precision = 19, scale = 4)
    private BigDecimal currentAmount;

    @Column(name = "current_validation_levels", nullable = false, updatable = false)
    private int currentValidationLevels;

    @Column(name = "actor_id", nullable = false, updatable = false, length = 150)
    private String actorId;

    @Column(name = "correlation_id", nullable = false, updatable = false, length = 150)
    private String correlationId;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt;

    protected PartnerThresholdHistoryJpaEntity() {
    }

    public PartnerThresholdHistoryJpaEntity(PartnerThresholdHistoryRecord record) {
        id = UUID.randomUUID();
        partnerId = record.partnerId().value();
        transactionType = record.currentThreshold().transactionType();
        currency = record.currentThreshold().currency();
        if (record.previousThreshold() != null) {
            previousAmount = record.previousThreshold().amount();
            previousValidationLevels = record.previousThreshold().validationLevels();
        }
        currentAmount = record.currentThreshold().amount();
        currentValidationLevels = record.currentThreshold().validationLevels();
        actorId = record.actorId();
        correlationId = record.correlationId();
        changedAt = record.changedAt();
    }
}
