package com.sixpay.payment.infrastructure.persistence.financial;

import com.sixpay.payment.domain.model.financial.FinancialEntryDirection;
import com.sixpay.payment.domain.model.financial.BankingEntryDetails;
import java.time.LocalDate;
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

    @Column(name = "bkmvti_age", updatable = false, length = 20)
    private String bkmvti_age;

    @Column(name = "bkmvti_cha", updatable = false, length = 20)
    private String bkmvti_cha;

    @Column(name = "bkmvti_ncp", updatable = false, length = 30)
    private String bkmvti_ncp;

    @Column(name = "bkmvti_suf", updatable = false, length = 10)
    private String bkmvti_suf;

    @Column(name = "bkmvti_clc", updatable = false, length = 10)
    private String bkmvti_clc;

    @Column(name = "bkmvti_ope", updatable = false, length = 3)
    private String bkmvti_ope;

    @Column(name = "bkmvti_mvti", updatable = false, length = 20)
    private String bkmvti_mvti;

    @Column(name = "bkmvti_uti", updatable = false, length = 64)
    private String bkmvti_uti;

    @Column(name = "bkmvti_eve", updatable = false, length = 32)
    private String bkmvti_eve;

    @Column(name = "bkmvti_dco", updatable = false)
    private LocalDate bkmvti_dco;

    @Column(name = "bkmvti_dva", updatable = false)
    private LocalDate bkmvti_dva;

    @Column(name = "bkmvti_lib", updatable = false, length = 256)
    private String bkmvti_lib;

    @Column(name = "bkmvti_exo", updatable = false, length = 10)
    private String bkmvti_exo;

    @Column(name = "bkmvti_pie", updatable = false, length = 100)
    private String bkmvti_pie;

    @Column(name = "bkmvti_agsa", updatable = false, length = 20)
    private String bkmvti_agsa;

    @Column(name = "bkmvti_agde", updatable = false, length = 20)
    private String bkmvti_agde;

    @Column(name = "bkmvti_devc", updatable = false, length = 3)
    private String bkmvti_devc;

    @Column(name = "bkmvti_mctv", updatable = false, precision = 38, scale = 18)
    private BigDecimal bkmvti_mctv;

    @Column(name = "bkmvti_nat", updatable = false, length = 32)
    private String bkmvti_nat;

    @Column(name = "bkmvti_eta", updatable = false, length = 10)
    private String bkmvti_eta;

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
        BankingEntryDetails details = snapshot.bankingDetails();
        if (details != null) {
            entity.bkmvti_age = details.age();
            entity.bkmvti_cha = details.cha();
            entity.bkmvti_ncp = details.ncp();
            entity.bkmvti_suf = details.suf();
            entity.bkmvti_clc = details.clc();
            entity.bkmvti_ope = details.ope();
            entity.bkmvti_mvti = details.mvti();
            entity.bkmvti_uti = details.uti();
            entity.bkmvti_eve = details.eve();
            entity.bkmvti_dco = details.dco();
            entity.bkmvti_dva = details.dva();
            entity.bkmvti_lib = details.lib();
            entity.bkmvti_exo = details.exo();
            entity.bkmvti_pie = details.pie();
            entity.bkmvti_agsa = details.agsa();
            entity.bkmvti_agde = details.agde();
            entity.bkmvti_devc = details.devc();
            entity.bkmvti_mctv = details.mctv();
            entity.bkmvti_nat = details.nat();
            entity.bkmvti_eta = details.eta();
        }
        return entity;
    }

    BankingEntryDetails bankingDetails() {
        if (bkmvti_age == null && bkmvti_cha == null && bkmvti_ncp == null && bkmvti_suf == null && bkmvti_clc == null && bkmvti_ope == null && bkmvti_mvti == null && bkmvti_uti == null && bkmvti_eve == null && bkmvti_dco == null && bkmvti_dva == null && bkmvti_lib == null && bkmvti_exo == null && bkmvti_pie == null && bkmvti_agsa == null && bkmvti_agde == null && bkmvti_devc == null && bkmvti_mctv == null && bkmvti_nat == null && bkmvti_eta == null) return null;
        return new BankingEntryDetails(
                bkmvti_age,
                bkmvti_cha,
                bkmvti_ncp,
                bkmvti_suf,
                bkmvti_clc,
                bkmvti_ope,
                bkmvti_mvti,
                bkmvti_uti,
                bkmvti_eve,
                bkmvti_dco,
                bkmvti_dva,
                bkmvti_lib,
                bkmvti_exo,
                bkmvti_pie,
                bkmvti_agsa,
                bkmvti_agde,
                bkmvti_devc,
                bkmvti_mctv,
                bkmvti_nat,
                bkmvti_eta);
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
