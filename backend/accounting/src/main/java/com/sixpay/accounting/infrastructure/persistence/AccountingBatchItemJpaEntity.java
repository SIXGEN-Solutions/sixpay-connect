package com.sixpay.accounting.infrastructure.persistence;

import com.sixpay.accounting.domain.model.AccountingBatchItem;
import com.sixpay.accounting.domain.model.AccountingBatchItemStatus;
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
import java.time.LocalDate;
import java.util.Currency;
import java.util.UUID;

@Entity
@Table(
        name = "accounting_batch_items",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_accounting_batch_items_payment_id",
                columnNames = "payment_id"
        )
)
public class AccountingBatchItemJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "batch_id",
            nullable = false,
            updatable = false
    )
    private AccountingBatchJpaEntity batch;

    @Column(
            name = "payment_id",
            nullable = false,
            updatable = false
    )
    private UUID paymentId;

    @Column(
            name = "public_payment_reference",
            nullable = false,
            updatable = false,
            length = 128
    )
    private String publicPaymentReference;

    @Column(
            name = "partner_id",
            nullable = false,
            updatable = false,
            length = 128
    )
    private String partnerId;

    @Column(
            name = "amount",
            nullable = false,
            updatable = false,
            precision = 19,
            scale = 4
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
            name = "payment_occurred_at",
            nullable = false,
            updatable = false
    )
    private Instant paymentOccurredAt;

    @Column(
            name = "payment_business_date",
            nullable = false,
            updatable = false
    )
    private LocalDate paymentBusinessDate;

    @Column(
            name = "bank_posting_reference",
            updatable = false,
            length = 128
    )
    private String bankPostingReference;

    @Column(
            name = "tresorpay_status",
            nullable = false,
            updatable = false,
            length = 64
    )
    private String tresorPayStatus;

    @Column(
            name = "tresorpay_status_checked_at",
            nullable = false,
            updatable = false
    )
    private Instant tresorPayStatusCheckedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private AccountingBatchItemStatus status;

    protected AccountingBatchItemJpaEntity() {
    }

    static AccountingBatchItemJpaEntity create(
            AccountingBatchJpaEntity batch,
            AccountingBatchItem item
    ) {
        var entity = new AccountingBatchItemJpaEntity();
        entity.id = UUID.randomUUID();
        entity.batch = batch;
        entity.paymentId = item.paymentId();
        entity.publicPaymentReference =
                item.publicPaymentReference();
        entity.partnerId = item.partnerId();
        entity.amount = item.amount();
        entity.currency = item.currency().getCurrencyCode();
        entity.paymentOccurredAt = item.paymentOccurredAt();
        entity.paymentBusinessDate = item.paymentBusinessDate();
        entity.bankPostingReference =
                item.bankPostingReference();
        entity.tresorPayStatus = item.tresorPayStatus();
        entity.tresorPayStatusCheckedAt =
                item.tresorPayStatusCheckedAt();
        entity.status = item.status();
        return entity;
    }

    public UUID paymentId() {
        return paymentId;
    }

    public String publicPaymentReference() {
        return publicPaymentReference;
    }

    public String partnerId() {
        return partnerId;
    }

    public BigDecimal amount() {
        return amount;
    }

    public Currency currency() {
        return Currency.getInstance(currency);
    }

    public Instant paymentOccurredAt() {
        return paymentOccurredAt;
    }

    public LocalDate paymentBusinessDate() {
        return paymentBusinessDate;
    }

    public String bankPostingReference() {
        return bankPostingReference;
    }

    public String tresorPayStatus() {
        return tresorPayStatus;
    }

    public Instant tresorPayStatusCheckedAt() {
        return tresorPayStatusCheckedAt;
    }

    public AccountingBatchItemStatus status() {
        return status;
    }
}
