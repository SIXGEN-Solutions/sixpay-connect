package com.sixpay.accounting.infrastructure.tfj.persistence;

import com.sixpay.accounting.domain.model.*;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
        name = "accounting_tfj_confirmations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_accounting_tfj_confirmation_idempotency",
                columnNames = "idempotency_key"
        )
)
public class AccountingTfjConfirmationJpaEntity {

    @Id
    @Column(name = "confirmation_id", nullable = false, updatable = false)
    private UUID confirmationId;

    @Column(name = "idempotency_key", nullable = false, updatable = false, length = 200)
    private String idempotencyKey;

    @Column(name = "logical_payload_hash", nullable = false, updatable = false, length = 64)
    private String logicalPayloadHash;

    @Column(name = "financial_institution_code", nullable = false, updatable = false, length = 35)
    private String financialInstitutionCode;

    @Column(name = "business_date", nullable = false, updatable = false)
    private LocalDate businessDate;

    @Column(name = "payment_reference", nullable = false, updatable = false, length = 100)
    private String paymentReference;

    @Column(name = "bank_posting_reference", nullable = false, updatable = false, length = 128)
    private String bankPostingReference;

    @Column(name = "tfj_batch_reference", updatable = false, length = 100)
    private String tfjBatchReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "tfj_status", nullable = false, updatable = false, length = 16)
    private TfjStatus status;

    @Column(name = "confirmed_at", nullable = false, updatable = false)
    private Instant confirmedAt;

    @Column(name = "failure_code", updatable = false, length = 64)
    private String failureCode;

    @Column(name = "failure_description", updatable = false, length = 500)
    private String failureDescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "recovery_action", updatable = false, length = 32)
    private TfjRecoveryAction recoveryAction;

    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "observation_channel", nullable = false, updatable = false, length = 32)
    private TfjObservationChannel observationChannel;

    @Column(name = "correlation_id", nullable = false, updatable = false, length = 128)
    private String correlationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_status", nullable = false, length = 16)
    private TfjMatchStatus matchStatus;

    @Column(name = "matched_payment_id")
    private UUID matchedPaymentId;

    @Column(name = "finality_published_at")
    private Instant finalityPublishedAt;

    protected AccountingTfjConfirmationJpaEntity() {
    }

    static AccountingTfjConfirmationJpaEntity fromDomain(
            TfjConfirmation source
    ) {
        var entity = new AccountingTfjConfirmationJpaEntity();
        entity.confirmationId = source.confirmationId();
        entity.idempotencyKey = source.idempotencyKey();
        entity.logicalPayloadHash = source.logicalPayloadHash();
        entity.financialInstitutionCode =
                source.financialInstitutionCode();
        entity.businessDate = source.businessDate();
        entity.paymentReference = source.paymentReference();
        entity.bankPostingReference =
                source.bankPostingReference();
        entity.tfjBatchReference = source.tfjBatchReference();
        entity.status = source.status();
        entity.confirmedAt = source.confirmedAt();
        entity.failureCode = source.failureCode();
        entity.failureDescription =
                source.failureDescription();
        entity.recoveryAction = source.recoveryAction();
        entity.receivedAt = source.receivedAt();
        entity.observationChannel =
                source.observationChannel();
        entity.correlationId = source.correlationId();
        entity.matchStatus = source.matchStatus();
        entity.matchedPaymentId = source.matchedPaymentId();
        entity.finalityPublishedAt =
                source.finalityPublishedAt();
        return entity;
    }

    void synchronizeMutableState(
            TfjConfirmation source
    ) {
        if (!confirmationId.equals(source.confirmationId())) {
            throw new IllegalArgumentException(
                    "Cannot change TFJ confirmationId"
            );
        }
        matchStatus = source.matchStatus();
        matchedPaymentId = source.matchedPaymentId();
        finalityPublishedAt =
                source.finalityPublishedAt();
    }

    TfjConfirmation toDomain() {
        return new TfjConfirmation(
                confirmationId,
                idempotencyKey,
                logicalPayloadHash,
                financialInstitutionCode,
                businessDate,
                paymentReference,
                bankPostingReference,
                tfjBatchReference,
                status,
                confirmedAt,
                failureCode,
                failureDescription,
                recoveryAction,
                receivedAt,
                observationChannel,
                correlationId,
                matchStatus,
                matchedPaymentId,
                finalityPublishedAt
        );
    }
}
