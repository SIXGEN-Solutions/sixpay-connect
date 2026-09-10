package com.sixpay.accounting.domain.model;

import com.sixpay.accounting.events.PaymentT0FinalizedForAccounting;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record AccountingCandidateProjection(
        UUID id,
        UUID eventId,
        UUID paymentId,
        String publicPaymentReference,
        String partnerId,
        String financialInstitutionCode,
        String bankReference,
        Instant t0ObservedAt,
        LocalDate accountingBusinessDate,
        UUID financialSnapshotId,
        String financialSnapshotVersion,
        Instant financialSnapshotFinalizedAt,
        String debtorAccountReference,
        String creditorAccountReference,
        BigDecimal amount,
        Currency currency,
        Instant paymentOccurredAt,
        Instant candidateCreatedAt,
        UUID batchId,
        TresorPayPaymentStatusEvidence tresorPayStatusEvidence,
        List<Entry> entries
) {
    public AccountingCandidateProjection {
        id = Objects.requireNonNull(id, "id");
        eventId = Objects.requireNonNull(eventId, "eventId");
        paymentId = Objects.requireNonNull(paymentId, "paymentId");
        publicPaymentReference = required(publicPaymentReference, "publicPaymentReference");
        partnerId = required(partnerId, "partnerId");
        financialInstitutionCode = required(financialInstitutionCode, "financialInstitutionCode");
        bankReference = required(bankReference, "bankReference");
        t0ObservedAt = Objects.requireNonNull(t0ObservedAt, "t0ObservedAt");
        accountingBusinessDate = Objects.requireNonNull(accountingBusinessDate, "accountingBusinessDate");
        financialSnapshotId = Objects.requireNonNull(financialSnapshotId, "financialSnapshotId");
        financialSnapshotVersion = required(financialSnapshotVersion, "financialSnapshotVersion");
        financialSnapshotFinalizedAt = Objects.requireNonNull(financialSnapshotFinalizedAt, "financialSnapshotFinalizedAt");
        debtorAccountReference = required(debtorAccountReference, "debtorAccountReference");
        creditorAccountReference = required(creditorAccountReference, "creditorAccountReference");
        amount = Objects.requireNonNull(amount, "amount");
        currency = Objects.requireNonNull(currency, "currency");
        paymentOccurredAt = Objects.requireNonNull(paymentOccurredAt, "paymentOccurredAt");
        candidateCreatedAt = Objects.requireNonNull(candidateCreatedAt, "candidateCreatedAt");
        entries = List.copyOf(Objects.requireNonNull(entries, "entries"));
    }

    public static AccountingCandidateProjection from(PaymentT0FinalizedForAccounting event, Instant projectedAt) {
        Objects.requireNonNull(event, "event");
        return new AccountingCandidateProjection(
                UUID.randomUUID(),
                event.eventId(),
                event.paymentId(),
                event.publicPaymentReference(),
                event.partnerId(),
                event.financialInstitutionCode(),
                event.bankReference(),
                event.t0ObservedAt(),
                event.accountingBusinessDate(),
                event.financialSnapshotId(),
                event.financialSnapshotVersion(),
                event.financialSnapshotFinalizedAt(),
                event.debtorAccountReference(),
                event.creditorAccountReference(),
                event.amount(),
                event.currency(),
                event.paymentOccurredAt(),
                Objects.requireNonNull(projectedAt, "projectedAt"),
                null,
                null,
                event.entries().stream().map(e -> new Entry(
                        e.entrySnapshotId(), e.sequence(), e.direction(),
                        e.accountReference(), e.amount(), e.currency(), e.createdAt()
                )).toList()
        );
    }

    public record Entry(
            UUID entrySnapshotId,
            int sequence,
            String direction,
            String accountReference,
            BigDecimal amount,
            Currency currency,
            Instant createdAt
    ) {}

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required");
        return value.strip();
    }
}
