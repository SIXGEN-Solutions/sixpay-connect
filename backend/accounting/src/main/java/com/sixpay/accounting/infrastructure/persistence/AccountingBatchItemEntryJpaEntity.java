package com.sixpay.accounting.infrastructure.persistence;

import com.sixpay.accounting.domain.model.AccountingBatchItemEntry;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.UUID;

@Entity
@Table(name = "accounting_batch_item_entries")
public class AccountingBatchItemEntryJpaEntity {
    @Id @Column(name = "id", nullable = false, updatable = false) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_item_id", nullable = false, updatable = false)
    private AccountingBatchItemJpaEntity item;
    @Column(name = "entry_snapshot_id", nullable = false, updatable = false) private UUID entrySnapshotId;
    @Column(name = "entry_sequence", nullable = false, updatable = false) private int sequence;
    @Column(name = "direction", nullable = false, updatable = false, length = 16) private String direction;
    @Column(name = "account_reference", nullable = false, updatable = false, length = 256) private String accountReference;
    @Column(name = "amount", nullable = false, updatable = false, precision = 19, scale = 4) private BigDecimal amount;
    @Column(name = "currency", nullable = false, updatable = false, length = 3) private String currency;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    protected AccountingBatchItemEntryJpaEntity() {}
    static AccountingBatchItemEntryJpaEntity create(AccountingBatchItemJpaEntity item, AccountingBatchItemEntry entry) {
        var e = new AccountingBatchItemEntryJpaEntity();
        e.id = UUID.randomUUID(); e.item = item; e.entrySnapshotId = entry.entrySnapshotId();
        e.sequence = entry.sequence(); e.direction = entry.direction(); e.accountReference = entry.accountReference();
        e.amount = entry.amount(); e.currency = entry.currency().getCurrencyCode(); e.createdAt = entry.createdAt();
        return e;
    }
    AccountingBatchItemEntry toDomain() {
        return new AccountingBatchItemEntry(entrySnapshotId, sequence, direction, accountReference,
                amount, Currency.getInstance(currency), createdAt);
    }
}
