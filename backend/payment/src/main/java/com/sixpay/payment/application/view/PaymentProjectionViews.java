package com.sixpay.payment.application.view;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class PaymentProjectionViews {
    private PaymentProjectionViews() {}

    public record MoneyView(BigDecimal amount, String currency) {
        public MoneyView {
            amount = Objects.requireNonNull(amount, "Amount");
            currency = requireText(currency, "Currency");
        }
    }

    public record MaskedAccountView(String reference, String maskedValue) {
        public MaskedAccountView {
            reference = requireText(reference, "Account reference");
            maskedValue = requireText(maskedValue, "Masked account value");
        }
    }

    public record Summary(
            UUID paymentId, String paymentReference,
            String tresorPayRequestId, UUID observedCustomerId,
            String financialInstitutionCode,
            MaskedAccountView debtorAccount, MoneyView amount,
            String status, String reasonCode, Instant createdAt,
            Instant updatedAt, Instant finalizedAt
    ) {
        public Summary {
            paymentId = Objects.requireNonNull(paymentId, "Payment ID");
            paymentReference = requireText(paymentReference, "Payment reference");
            tresorPayRequestId = requireText(tresorPayRequestId, "TRESOR PAY request ID");
            financialInstitutionCode = requireText(financialInstitutionCode, "Financial institution code");
            amount = Objects.requireNonNull(amount, "Payment amount");
            status = requireText(status, "Payment status");
            createdAt = Objects.requireNonNull(createdAt, "Created instant");
            updatedAt = Objects.requireNonNull(updatedAt, "Updated instant");
        }
    }

    public record SearchPage(
            List<Summary> items, int size, boolean hasMore,
            String nextCursor, Instant snapshotAt
    ) {
        public SearchPage {
            items = List.copyOf(Objects.requireNonNull(items, "Payment items"));
            if (size < 0) throw new IllegalArgumentException("Page size must not be negative");
            snapshotAt = Objects.requireNonNull(snapshotAt, "Snapshot instant");
        }
    }

    public record BankingVerification(String verificationId, String outcome, List<String> reasonCodes, Instant observedAt) {}
    public record Posting(String bankPostingReference, String outcome, Instant observedAt) {}
    public record Tfj(String status, LocalDate businessDate, Instant confirmedAt) {}
    public record Notification(String type, String status, UUID eventId, Instant lastAttemptAt) {}
    public record Reversal(String status, String reversalReference, Instant observedAt) {}

    public record Detail(
            Summary summary, UUID correlationId, long aggregateVersion,
            BankingVerification bankingVerification, Posting posting,
            Tfj tfj, List<Notification> notifications, Reversal reversal
    ) {
        public Detail {
            summary = Objects.requireNonNull(summary, "Payment summary");
            correlationId = Objects.requireNonNull(correlationId, "Correlation ID");
            if (aggregateVersion < 0) throw new IllegalArgumentException("Aggregate version must not be negative");
            notifications = notifications == null ? List.of() : List.copyOf(notifications);
        }
    }

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return value;
    }
}
