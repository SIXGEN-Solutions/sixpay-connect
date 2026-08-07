package com.sixpay.accounting.infrastructure.persistence;

import com.sixpay.accounting.domain.model.AccountingBatchItemTracking;
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
@Table(name = "accounting_batch_item_tracking")
public class AccountingBatchItemTrackingJpaEntity {

    @Id
    @Column(
            name = "payment_id",
            nullable = false,
            updatable = false
    )
    private UUID paymentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "batch_id",
            nullable = false,
            updatable = false
    )
    private AccountingBatchTrackingJpaEntity tracking;

    @Column(
            name = "provider_item_reference",
            length = 128
    )
    private String providerItemReference;

    @Column(name = "rejection_code", length = 128)
    private String rejectionCode;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AccountingBatchItemTrackingJpaEntity() {
    }

    static AccountingBatchItemTrackingJpaEntity create(
            AccountingBatchTrackingJpaEntity tracking,
            AccountingBatchItemTracking item
    ) {
        var entity = new AccountingBatchItemTrackingJpaEntity();
        entity.paymentId = item.paymentId();
        entity.tracking = tracking;
        entity.synchronize(item);
        return entity;
    }

    void synchronize(
            AccountingBatchItemTracking item
    ) {
        if (!paymentId.equals(item.paymentId())) {
            throw new IllegalArgumentException(
                    "Cannot change tracked paymentId"
            );
        }

        providerItemReference =
                item.providerItemReference();
        rejectionCode = item.rejectionCode();
        updatedAt = item.updatedAt();
    }

    UUID paymentId() {
        return paymentId;
    }

    String providerItemReference() {
        return providerItemReference;
    }

    String rejectionCode() {
        return rejectionCode;
    }

    Instant updatedAt() {
        return updatedAt;
    }
}
