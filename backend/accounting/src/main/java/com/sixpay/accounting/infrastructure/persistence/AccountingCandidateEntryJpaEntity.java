package com.sixpay.accounting.infrastructure.persistence;

import com.sixpay.accounting.domain.model.AccountingCandidateProjection;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.UUID;

@Entity
@Table(name = "accounting_payment_candidate_entries")
public class AccountingCandidateEntryJpaEntity {
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_id", nullable = false, updatable = false)
    private AccountingCandidateJpaEntity candidate;

    @Column(name = "entry_snapshot_id", nullable = false, updatable = false)
    private UUID entrySnapshotId;

    @Column(name = "entry_sequence", nullable = false, updatable = false)
    private int sequence;

    @Column(name = "direction", nullable = false, updatable = false, length = 16)
    private String direction;

    @Column(name = "account_reference", nullable = false, updatable = false, length = 256)
    private String accountReference;

    @Column(name = "amount", nullable = false, updatable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, updatable = false, length = 3)
    private String currency;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AccountingCandidateEntryJpaEntity() {}

    static AccountingCandidateEntryJpaEntity create(AccountingCandidateJpaEntity candidate, AccountingCandidateProjection.Entry entry) {
        var e = new AccountingCandidateEntryJpaEntity();
        e.id = UUID.randomUUID();
        e.candidate = candidate;
        e.entrySnapshotId = entry.entrySnapshotId();
        e.sequence = entry.sequence();
        e.direction = entry.direction();
        e.accountReference = entry.accountReference();
        e.amount = entry.amount();
        e.currency = entry.currency().getCurrencyCode();
        e.createdAt = entry.createdAt();
        return e;
    }

    AccountingCandidateProjection.Entry toDomain() {
        return new AccountingCandidateProjection.Entry(
                entrySnapshotId, sequence, direction, accountReference,
                amount, Currency.getInstance(currency), createdAt
        );
    }
}
