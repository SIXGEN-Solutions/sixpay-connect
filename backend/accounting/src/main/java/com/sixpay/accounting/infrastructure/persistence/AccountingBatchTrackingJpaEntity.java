package com.sixpay.accounting.infrastructure.persistence;

import com.sixpay.accounting.domain.model.AccountingBatchItemTracking;
import com.sixpay.accounting.domain.model.AccountingBatchTracking;
import com.sixpay.accounting.domain.model.AccountingSubmissionState;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "accounting_batch_tracking")
public class AccountingBatchTrackingJpaEntity {

    @Id
    @Column(name = "batch_id", nullable = false, updatable = false)
    private UUID batchId;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "submission_state",
            nullable = false,
            length = 48
    )
    private AccountingSubmissionState submissionState;

    @Column(
            name = "provider_batch_reference",
            length = 128
    )
    private String providerBatchReference;

    @Column(name = "last_submission_attempt_at")
    private Instant lastSubmissionAttemptAt;

    @Column(name = "last_reconciliation_at")
    private Instant lastReconciliationAt;

    @Column(
            name = "reconciliation_attempts",
            nullable = false
    )
    private int reconciliationAttempts;

    @Column(name = "last_error_code", length = 128)
    private String lastErrorCode;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @OneToMany(
            mappedBy = "tracking",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<AccountingBatchItemTrackingJpaEntity> items =
            new ArrayList<>();

    protected AccountingBatchTrackingJpaEntity() {
    }

    static AccountingBatchTrackingJpaEntity create(
            AccountingBatchTracking tracking
    ) {
        var entity = new AccountingBatchTrackingJpaEntity();
        entity.batchId = tracking.batchId().value();
        entity.synchronize(tracking);
        return entity;
    }

    void synchronize(
            AccountingBatchTracking tracking
    ) {
        submissionState = tracking.submissionState();
        providerBatchReference =
                tracking.providerBatchReference();
        lastSubmissionAttemptAt =
                tracking.lastSubmissionAttemptAt();
        lastReconciliationAt =
                tracking.lastReconciliationAt();
        reconciliationAttempts =
                tracking.reconciliationAttempts();
        lastErrorCode = tracking.lastErrorCode();

        Map<UUID, AccountingBatchItemTrackingJpaEntity> existing =
                new HashMap<>();

        for (AccountingBatchItemTrackingJpaEntity item : items) {
            existing.put(item.paymentId(), item);
        }

        for (AccountingBatchItemTracking item : tracking.items()) {
            AccountingBatchItemTrackingJpaEntity entity =
                    existing.remove(item.paymentId());

            if (entity == null) {
                items.add(
                        AccountingBatchItemTrackingJpaEntity
                                .create(
                                        this,
                                        item
                                )
                );
            } else {
                entity.synchronize(item);
            }
        }

        if (!existing.isEmpty()) {
            items.removeIf(item ->
                    existing.containsKey(
                            item.paymentId()
                    )
            );
        }
    }

    UUID batchId() {
        return batchId;
    }

    AccountingSubmissionState submissionState() {
        return submissionState;
    }

    String providerBatchReference() {
        return providerBatchReference;
    }

    Instant lastSubmissionAttemptAt() {
        return lastSubmissionAttemptAt;
    }

    Instant lastReconciliationAt() {
        return lastReconciliationAt;
    }

    int reconciliationAttempts() {
        return reconciliationAttempts;
    }

    String lastErrorCode() {
        return lastErrorCode;
    }

    List<AccountingBatchItemTrackingJpaEntity> items() {
        return List.copyOf(items);
    }
}
