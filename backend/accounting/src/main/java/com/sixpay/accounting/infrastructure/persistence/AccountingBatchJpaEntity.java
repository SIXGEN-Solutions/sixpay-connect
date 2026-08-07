package com.sixpay.accounting.infrastructure.persistence;

import com.sixpay.accounting.domain.model.AccountingBatch;
import com.sixpay.accounting.domain.model.AccountingBatchStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "accounting_batches",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_accounting_batches_idempotency_key",
                columnNames = "idempotency_key"
        )
)
public class AccountingBatchJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(
            name = "idempotency_key",
            nullable = false,
            updatable = false,
            length = 64
    )
    private String idempotencyKey;

    @Column(
            name = "business_date",
            nullable = false,
            updatable = false
    )
    private LocalDate businessDate;

    @Column(
            name = "financial_institution_code",
            nullable = false,
            updatable = false,
            length = 64
    )
    private String financialInstitutionCode;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 32
    )
    private AccountingBatchStatus status;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @OneToMany(
            mappedBy = "batch",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<AccountingBatchItemJpaEntity> items =
            new ArrayList<>();

    protected AccountingBatchJpaEntity() {
    }

    public static AccountingBatchJpaEntity create(
            AccountingBatch batch
    ) {
        var entity = new AccountingBatchJpaEntity();
        entity.id = batch.batchId().value();
        entity.idempotencyKey = batch.idempotencyKey().value();
        entity.businessDate = batch.businessDate();
        entity.financialInstitutionCode =
                batch.financialInstitutionCode();
        entity.createdAt = batch.createdAt();
        entity.status = batch.status();

        batch.items().stream()
                .map(item ->
                        AccountingBatchItemJpaEntity.create(
                                entity,
                                item
                        )
                )
                .forEach(entity.items::add);

        return entity;
    }

    public void synchronize(AccountingBatch batch) {
        status = batch.status();

        items.clear();
        batch.items().stream()
                .map(item ->
                        AccountingBatchItemJpaEntity.create(
                                this,
                                item
                        )
                )
                .forEach(items::add);
    }

    public UUID id() {
        return id;
    }

    public String idempotencyKey() {
        return idempotencyKey;
    }

    public LocalDate businessDate() {
        return businessDate;
    }

    public String financialInstitutionCode() {
        return financialInstitutionCode;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public AccountingBatchStatus status() {
        return status;
    }

    public List<AccountingBatchItemJpaEntity> items() {
        return List.copyOf(items);
    }
}
