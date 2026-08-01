package com.sixpay.payment.infrastructure.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class PaymentQueryResponses {
    private PaymentQueryResponses() {}
    public record MoneyResponse(BigDecimal amount, String currency) {}
    public record MaskedAccountResponse(String reference, String maskedValue) {}
    public record PaymentSummaryResponse(UUID paymentId, String paymentReference, String tresorPayRequestId, UUID observedCustomerId, String financialInstitutionCode, MaskedAccountResponse debtorAccount, MoneyResponse amount, String status, String reasonCode, Instant createdAt, Instant updatedAt, Instant finalizedAt) {}
    public record PaymentSearchPageResponse(List<PaymentSummaryResponse> items, int size, boolean hasMore, String nextCursor, Instant snapshotAt) {}
    public record BankingVerificationResponse(String verificationId, String outcome, List<String> reasonCodes, Instant observedAt) {}
    public record PostingResponse(String bankPostingReference, String outcome, Instant observedAt) {}
    public record TfjResponse(String status, LocalDate businessDate, Instant confirmedAt) {}
    public record NotificationResponse(String type, String status, UUID eventId, Instant lastAttemptAt) {}
    public record ReversalResponse(String status, String reversalReference, Instant observedAt) {}
    public record PaymentDetailResponse(UUID paymentId, String paymentReference, String tresorPayRequestId, UUID observedCustomerId, String financialInstitutionCode, MaskedAccountResponse debtorAccount, MoneyResponse amount, String status, String reasonCode, Instant createdAt, Instant updatedAt, Instant finalizedAt, UUID correlationId, long aggregateVersion, BankingVerificationResponse bankingVerification, PostingResponse posting, TfjResponse tfj, List<NotificationResponse> notifications, ReversalResponse reversal) {}
}
