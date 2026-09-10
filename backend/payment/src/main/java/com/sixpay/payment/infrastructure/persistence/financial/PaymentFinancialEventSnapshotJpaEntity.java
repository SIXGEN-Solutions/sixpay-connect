package com.sixpay.payment.infrastructure.persistence.financial;

import com.sixpay.payment.domain.model.financial.FinancialSnapshotStatus;
import com.sixpay.payment.domain.model.financial.PaymentFinancialEventSnapshot;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "payment_financial_event_snapshots",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_payment_fin_event_payment",
                columnNames = "payment_id"
        )
)
public class PaymentFinancialEventSnapshotJpaEntity {

    @Id
    @Column(
            name = "snapshot_id",
            nullable = false,
            updatable = false
    )
    private UUID snapshotId;

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
            length = 30
    )
    private String publicPaymentReference;

    @Column(
            name = "financial_institution_code",
            nullable = false,
            updatable = false,
            length = 32
    )
    private String financialInstitutionCode;

    @Column(
            name = "debtor_account_reference",
            nullable = false,
            updatable = false,
            length = 256
    )
    private String debtorAccountReference;

    @Column(
            name = "creditor_account_reference",
            nullable = false,
            updatable = false,
            length = 256
    )
    private String creditorAccountReference;

    @Column(
            name = "requested_amount",
            nullable = false,
            updatable = false,
            precision = 38,
            scale = 18
    )
    private BigDecimal requestedAmount;

    @Column(
            name = "requested_currency",
            nullable = false,
            updatable = false,
            length = 3
    )
    private String requestedCurrency;

    @Column(
            name = "snapshot_version",
            nullable = false,
            updatable = false,
            length = 32
    )
    private String snapshotVersion;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "snapshot_status",
            nullable = false,
            updatable = false,
            length = 16
    )
    private FinancialSnapshotStatus snapshotStatus;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @Column(
            name = "finalized_at",
            nullable = false,
            updatable = false
    )
    private Instant finalizedAt;

    @OneToMany(
            mappedBy = "eventSnapshot",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY
    )
    private List<PaymentFinancialEntrySnapshotJpaEntity>
            entries = new ArrayList<>();

    protected PaymentFinancialEventSnapshotJpaEntity() {
    }

    static PaymentFinancialEventSnapshotJpaEntity create(
            PaymentFinancialEventSnapshot snapshot
    ) {
        if (snapshot.status()
                != FinancialSnapshotStatus.FINALIZED) {
            throw new IllegalArgumentException(
                    "Only FINALIZED financial snapshots "
                            + "may be persisted"
            );
        }

        var entity =
                new PaymentFinancialEventSnapshotJpaEntity();
        entity.snapshotId = snapshot.snapshotId();
        entity.paymentId = snapshot.paymentId().value();
        entity.publicPaymentReference =
                snapshot.publicPaymentReference().value();
        entity.financialInstitutionCode =
                snapshot.financialInstitutionCode().value();
        entity.debtorAccountReference =
                snapshot.debtorAccountReference();
        entity.creditorAccountReference =
                snapshot.creditorAccountReference();
        entity.requestedAmount =
                snapshot.requestedAmount().amount();
        entity.requestedCurrency =
                snapshot.requestedAmount()
                        .currency()
                        .getCurrencyCode();
        entity.snapshotVersion =
                snapshot.snapshotVersion();
        entity.snapshotStatus = snapshot.status();
        entity.createdAt = snapshot.createdAt();
        entity.finalizedAt =
                snapshot.finalizedAt().orElseThrow();

        snapshot.entries().stream()
                .map(entry ->
                        PaymentFinancialEntrySnapshotJpaEntity
                                .create(entity, entry)
                )
                .forEach(entity.entries::add);

        return entity;
    }

    UUID snapshotId() {
        return snapshotId;
    }

    UUID paymentId() {
        return paymentId;
    }

    String publicPaymentReference() {
        return publicPaymentReference;
    }

    String financialInstitutionCode() {
        return financialInstitutionCode;
    }

    String debtorAccountReference() {
        return debtorAccountReference;
    }

    String creditorAccountReference() {
        return creditorAccountReference;
    }

    BigDecimal requestedAmount() {
        return requestedAmount;
    }

    String requestedCurrency() {
        return requestedCurrency;
    }

    String snapshotVersion() {
        return snapshotVersion;
    }

    FinancialSnapshotStatus snapshotStatus() {
        return snapshotStatus;
    }

    Instant createdAt() {
        return createdAt;
    }

    Instant finalizedAt() {
        return finalizedAt;
    }

    List<PaymentFinancialEntrySnapshotJpaEntity> entries() {
        return List.copyOf(entries);
    }
}
