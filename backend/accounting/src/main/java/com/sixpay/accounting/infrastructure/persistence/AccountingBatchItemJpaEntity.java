package com.sixpay.accounting.infrastructure.persistence;

import com.sixpay.accounting.domain.model.AccountingBatchItem;
import com.sixpay.accounting.domain.model.AccountingBatchItemStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "accounting_batch_items",
        uniqueConstraints = @UniqueConstraint(name = "uk_accounting_batch_items_payment_id", columnNames = "payment_id"))
public class AccountingBatchItemJpaEntity {
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id", nullable = false, updatable = false)
    private AccountingBatchJpaEntity batch;
    @Column(name = "payment_id", nullable = false, updatable = false)
    private UUID paymentId;
    @Column(name = "public_payment_reference", nullable = false, updatable = false, length = 128)
    private String publicPaymentReference;
    @Column(name = "partner_id", nullable = false, updatable = false, length = 128)
    private String partnerId;
    @Column(name = "amount", nullable = false, updatable = false, precision = 19, scale = 4)
    private BigDecimal amount;
    @Column(name = "currency", nullable = false, updatable = false, length = 3)
    private String currency;
    @Column(name = "payment_occurred_at", nullable = false, updatable = false)
    private Instant paymentOccurredAt;
    @Column(name = "payment_business_date", nullable = false, updatable = false)
    private LocalDate paymentBusinessDate;
    @Column(name = "bank_posting_reference", updatable = false, length = 128)
    private String bankPostingReference;
    @Column(name = "financial_snapshot_id", updatable = false)
    private UUID financialSnapshotId;
    @Column(name = "financial_snapshot_version", updatable = false, length = 32)
    private String financialSnapshotVersion;
    @Column(name = "financial_snapshot_finalized_at", updatable = false)
    private Instant financialSnapshotFinalizedAt;
    @Column(name = "debtor_account_reference", updatable = false, length = 256)
    private String debtorAccountReference;
    @Column(name = "creditor_account_reference", updatable = false, length = 256)
    private String creditorAccountReference;
    @Column(name = "tresorpay_status", nullable = false, updatable = false, length = 64)
    private String tresorPayStatus;
    @Column(name = "tresorpay_status_checked_at", nullable = false, updatable = false)
    private Instant tresorPayStatusCheckedAt;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private AccountingBatchItemStatus status;

    @OneToMany(mappedBy = "batchItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AccountingBatchItemEntryJpaEntity> frozenEntries = new ArrayList<>();

    protected AccountingBatchItemJpaEntity() {}

    static AccountingBatchItemJpaEntity create(AccountingBatchJpaEntity batch, AccountingBatchItem item) {
        var e = new AccountingBatchItemJpaEntity();
        e.id = UUID.randomUUID();
        e.batch = batch;
        e.paymentId = item.paymentId();
        e.publicPaymentReference = item.publicPaymentReference();
        e.partnerId = item.partnerId();
        e.amount = item.amount();
        e.currency = item.currency().getCurrencyCode();
        e.paymentOccurredAt = item.paymentOccurredAt();
        e.paymentBusinessDate = item.paymentBusinessDate();
        e.bankPostingReference = item.bankPostingReference();
        e.financialSnapshotId = item.financialSnapshotId();
        e.financialSnapshotVersion = item.financialSnapshotVersion();
        e.financialSnapshotFinalizedAt = item.financialSnapshotFinalizedAt();
        e.debtorAccountReference = item.debtorAccountReference();
        e.creditorAccountReference = item.creditorAccountReference();
        e.tresorPayStatus = item.tresorPayStatus();
        e.tresorPayStatusCheckedAt = item.tresorPayStatusCheckedAt();
        e.status = item.status();
        item.frozenEntries().stream()
                .map(entry -> AccountingBatchItemEntryJpaEntity.create(e, entry))
                .forEach(e.frozenEntries::add);
        return e;
    }

    void synchronize(AccountingBatchItem item) {
        if (!paymentId.equals(item.paymentId())) {
            throw new IllegalArgumentException("Cannot change accounting item paymentId");
        }
        if (financialSnapshotId != null && item.financialSnapshotId() != null
                && !financialSnapshotId.equals(item.financialSnapshotId())) {
            throw new IllegalArgumentException("Cannot change accounting item financialSnapshotId");
        }
        status = item.status();
    }

    AccountingBatchItem toDomain() {
        if (financialSnapshotId == null) {
            return new AccountingBatchItem(
                    paymentId, publicPaymentReference, partnerId, amount, Currency.getInstance(currency),
                    paymentOccurredAt, paymentBusinessDate, bankPostingReference,
                    tresorPayStatus, tresorPayStatusCheckedAt, status);
        }
        return new AccountingBatchItem(
                paymentId, publicPaymentReference, partnerId, amount, Currency.getInstance(currency),
                paymentOccurredAt, paymentBusinessDate, bankPostingReference,
                financialSnapshotId, financialSnapshotVersion, financialSnapshotFinalizedAt,
                debtorAccountReference, creditorAccountReference,
                frozenEntries.stream().map(AccountingBatchItemEntryJpaEntity::toDomain).toList(),
                tresorPayStatus, tresorPayStatusCheckedAt, status);
    }

    public UUID paymentId() { return paymentId; }
    public String publicPaymentReference() { return publicPaymentReference; }
    public String partnerId() { return partnerId; }
    public BigDecimal amount() { return amount; }
    public Currency currency() { return Currency.getInstance(currency); }
    public Instant paymentOccurredAt() { return paymentOccurredAt; }
    public LocalDate paymentBusinessDate() { return paymentBusinessDate; }
    public String bankPostingReference() { return bankPostingReference; }
    public String tresorPayStatus() { return tresorPayStatus; }
    public Instant tresorPayStatusCheckedAt() { return tresorPayStatusCheckedAt; }
    public AccountingBatchItemStatus status() { return status; }
}
