package com.sixpay.payment.infrastructure.persistence.financial;

import com.sixpay.payment.domain.model.financial.FinancialEntryDirection;
import com.sixpay.payment.domain.model.financial.PaymentFinancialEntrySnapshot;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "payment_financial_entry_snapshots",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_payment_fin_entry_event_sequence",
                columnNames = {
                        "event_snapshot_id",
                        "entry_sequence"
                }
        )
)
class PaymentFinancialEntrySnapshotJpaEntity {

    @Id
    @Column(
            name = "entry_snapshot_id",
            nullable = false,
            updatable = false
    )
    private UUID entrySnapshotId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "event_snapshot_id",
            nullable = false,
            updatable = false
    )
    private PaymentFinancialEventSnapshotJpaEntity eventSnapshot;

    @Column(
            name = "entry_sequence",
            nullable = false,
            updatable = false
    )
    private int sequence;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "direction",
            nullable = false,
            updatable = false,
            length = 16
    )
    private FinancialEntryDirection direction;

    @Column(
            name = "account_reference",
            nullable = false,
            updatable = false,
            length = 256
    )
    private String accountReference;

    @Column(
            name = "amount",
            nullable = false,
            updatable = false,
            precision = 38,
            scale = 18
    )
    private BigDecimal amount;

    @Column(
            name = "currency",
            nullable = false,
            updatable = false,
            length = 3
    )
    private String currency;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    protected PaymentFinancialEntrySnapshotJpaEntity() {
    }

    static PaymentFinancialEntrySnapshotJpaEntity create(
            PaymentFinancialEventSnapshotJpaEntity eventSnapshot,
            PaymentFinancialEntrySnapshot snapshot
    ) {
        var entity =
                new PaymentFinancialEntrySnapshotJpaEntity();
        entity.entrySnapshotId =
                snapshot.entrySnapshotId();
        entity.eventSnapshot = eventSnapshot;
        entity.sequence = snapshot.sequence();
        entity.direction = snapshot.direction();
        entity.accountReference =
                snapshot.accountReference();
        entity.amount = snapshot.amount().amount();
        entity.currency = snapshot.amount()
                .currency()
                .getCurrencyCode();
        entity.createdAt = snapshot.createdAt();
        return entity;
    }

    UUID entrySnapshotId() {
        return entrySnapshotId;
    }

    int sequence() {
        return sequence;
    }

    FinancialEntryDirection direction() {
        return direction;
    }

    String accountReference() {
        return accountReference;
    }

    BigDecimal amount() {
        return amount;
    }

    String currency() {
        return currency;
    }

    Instant createdAt() {
        return createdAt;
    }
}
