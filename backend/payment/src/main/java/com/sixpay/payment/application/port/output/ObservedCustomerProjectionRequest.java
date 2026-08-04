package com.sixpay.payment.application.port.output;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Payment-owned, transport-neutral projection request.
 *
 * <p>No Customer, JPA, HTTP, Amplitude or outbox entity type crosses this
 * boundary.</p>
 */
public record ObservedCustomerProjectionRequest(
        UUID sourceEventId,
        UUID paymentId,
        String paymentReference,
        String normalizedNiu,
        String legalName,
        String phoneMasked,
        String emailMasked,
        String financialInstitutionCode,
        String accountBindingFingerprint,
        String maskedAccountReference,
        BigDecimal amount,
        String currency,
        ProjectionPaymentStatus paymentStatus,
        String failureReasonCode,
        Instant paymentCreatedAt,
        Instant paymentUpdatedAt,
        Instant observedAt,
        String correlationId
) {

    private static final UUID NIL_UUID = new UUID(0L, 0L);

    public ObservedCustomerProjectionRequest {
        sourceEventId = requireUuid(sourceEventId, "sourceEventId");
        paymentId = requireUuid(paymentId, "paymentId");
        paymentReference = requireText(
                paymentReference,
                "paymentReference"
        );
        normalizedNiu = requireText(
                normalizedNiu,
                "normalizedNiu"
        );
        legalName = requireText(legalName, "legalName");
        financialInstitutionCode = requireText(
                financialInstitutionCode,
                "financialInstitutionCode"
        );
        accountBindingFingerprint = requireText(
                accountBindingFingerprint,
                "accountBindingFingerprint"
        );
        maskedAccountReference = requireText(
                maskedAccountReference,
                "maskedAccountReference"
        );
        amount = Objects.requireNonNull(amount, "amount is required");
        currency = requireText(currency, "currency");
        paymentStatus = Objects.requireNonNull(
                paymentStatus,
                "paymentStatus is required"
        );
        paymentCreatedAt = Objects.requireNonNull(
                paymentCreatedAt,
                "paymentCreatedAt is required"
        );
        paymentUpdatedAt = Objects.requireNonNull(
                paymentUpdatedAt,
                "paymentUpdatedAt is required"
        );
        observedAt = Objects.requireNonNull(
                observedAt,
                "observedAt is required"
        );
        correlationId = requireText(
                correlationId,
                "correlationId"
        );
    }

    private static UUID requireUuid(
            UUID value,
            String field
    ) {
        Objects.requireNonNull(value, field + " is required");
        if (NIL_UUID.equals(value)) {
            throw new IllegalArgumentException(
                    field + " must not be nil"
            );
        }
        return value;
    }

    private static String requireText(
            String value,
            String field
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    field + " must not be blank"
            );
        }
        return value.strip();
    }

    @Override
    public String toString() {
        return "ObservedCustomerProjectionRequest[sourceEventId="
                + sourceEventId
                + ", paymentId="
                + paymentId
                + ", paymentReference="
                + paymentReference
                + ", normalizedNiu=[PROTECTED]"
                + ", legalName=[PROTECTED]"
                + ", phoneMasked=[PROTECTED]"
                + ", emailMasked=[PROTECTED]"
                + ", financialInstitutionCode="
                + financialInstitutionCode
                + ", accountBindingFingerprint=[PROTECTED]"
                + ", maskedAccountReference=[PROTECTED]"
                + ", amount="
                + amount
                + ", currency="
                + currency
                + ", paymentStatus="
                + paymentStatus
                + ", failureReasonCode="
                + failureReasonCode
                + ", paymentCreatedAt="
                + paymentCreatedAt
                + ", paymentUpdatedAt="
                + paymentUpdatedAt
                + ", observedAt="
                + observedAt
                + ", correlationId="
                + correlationId
                + "]";
    }

    public enum ProjectionPaymentStatus {
        RECEIVED,
        AUTHORIZATION_CHECKING,
        BANKING_CHECKING,
        REJECTED,
        APPROVED,
        POSTING,
        ACCOUNTING_OUTCOME_UNKNOWN,
        DEBITED,
        CUT_CREDITED,
        REVERSAL_REQUIRED,
        REVERSAL_PENDING,
        REVERSED,
        FAILED,
        NOTIFIED,
        PENDING_END_OF_DAY_CONFIRMATION,
        TREASURY_INTEGRATED
    }
}
