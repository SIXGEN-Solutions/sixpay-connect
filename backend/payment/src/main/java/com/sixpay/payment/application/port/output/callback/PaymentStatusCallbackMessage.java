package com.sixpay.payment.application.port.output.callback;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public record PaymentStatusCallbackMessage(
        String schemaVersion,
        UUID eventId,
        String eventType,
        Instant occurredAt,
        UUID correlationId,
        UUID causationId,
        UUID paymentId,
        String paymentReference,
        String externalPaymentReference,
        String financialInstitutionCode,
        long paymentVersion,
        Object data
) {
    public static final String CUT_CREDITED = "CUT_CREDITED";
    public static final String TREASURY_INTEGRATED = "TREASURY_INTEGRATED";

    public PaymentStatusCallbackMessage {
        schemaVersion = requireText(schemaVersion, "Schema version");
        if (!"1.0".equals(schemaVersion)) {
            throw new IllegalArgumentException("Callback schema version must be 1.0");
        }
        eventId = Objects.requireNonNull(eventId, "Event ID");
        eventType = requireText(eventType, "Event type");
        if (!CUT_CREDITED.equals(eventType) && !TREASURY_INTEGRATED.equals(eventType)) {
            throw new IllegalArgumentException("Unsupported callback event type: " + eventType);
        }
        occurredAt = Objects.requireNonNull(occurredAt, "Occurrence instant");
        correlationId = Objects.requireNonNull(correlationId, "Correlation ID");
        paymentId = Objects.requireNonNull(paymentId, "Payment ID");
        paymentReference = requireText(paymentReference, "Payment reference");
        externalPaymentReference = requireText(externalPaymentReference, "External Payment reference");
        financialInstitutionCode = requireText(financialInstitutionCode, "Financial institution code");
        if (paymentVersion <= 0) {
            throw new IllegalArgumentException("Payment version must be positive");
        }
        data = Objects.requireNonNull(data, "Callback data");
        if (CUT_CREDITED.equals(eventType) && !(data instanceof CutCreditedData)) {
            throw new IllegalArgumentException("CUT_CREDITED requires CutCreditedData");
        }
        if (TREASURY_INTEGRATED.equals(eventType) && !(data instanceof TreasuryIntegratedData)) {
            throw new IllegalArgumentException("TREASURY_INTEGRATED requires TreasuryIntegratedData");
        }
    }

    public record MoneyData(String amount, String currency) {
        public MoneyData {
            amount = requireText(amount, "Amount");
            currency = requireText(currency, "Currency");
        }
    }

    public record CutCreditedData(
            MoneyData amount,
            String bankPostingReference,
            Instant creditedAt,
            String immediateResult,
            boolean tfjFinality
    ) {
        public CutCreditedData {
            amount = Objects.requireNonNull(amount, "Amount");
            bankPostingReference = requireText(bankPostingReference, "Bank posting reference");
            creditedAt = Objects.requireNonNull(creditedAt, "Credited instant");
            immediateResult = requireText(immediateResult, "Immediate result");
            if (!"POSTED_PENDING_TFJ".equals(immediateResult)) {
                throw new IllegalArgumentException("CUT_CREDITED immediateResult must be POSTED_PENDING_TFJ");
            }
            if (tfjFinality) {
                throw new IllegalArgumentException("CUT_CREDITED must not establish TFJ finality");
            }
        }
    }

    public record TreasuryIntegratedData(
            LocalDate businessDate,
            String bankPostingReference,
            UUID tfjConfirmationId,
            String tfjBatchReference,
            Instant integratedAt
    ) {
        public TreasuryIntegratedData {
            businessDate = Objects.requireNonNull(businessDate, "Business date");
            bankPostingReference = requireText(bankPostingReference, "Bank posting reference");
            tfjConfirmationId = Objects.requireNonNull(tfjConfirmationId, "TFJ confirmation ID");
            if (tfjBatchReference != null) {
                tfjBatchReference = requireText(tfjBatchReference, "TFJ batch reference");
            }
            integratedAt = Objects.requireNonNull(integratedAt, "Integrated instant");
        }
    }

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return value.trim();
    }
}
