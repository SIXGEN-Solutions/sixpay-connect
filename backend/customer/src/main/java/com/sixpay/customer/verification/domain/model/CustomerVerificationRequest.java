package com.sixpay.customer.verification.domain.model;

import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable business intention to perform a fresh Customer Verification.
 *
 * <p>The request is transport-neutral and contains no HTTP DTO, header,
 * authentication token, Payment implementation object or Amplitude client.</p>
 *
 * @param verificationId stable verification identifier
 * @param subject customer identity targeted by verification
 * @param financialInstitutionCode institution responsible for evidence
 * @param accountBindingFingerprint protected debtor-account binding
 * @param context safe correlation metadata
 * @param requestedAt explicit request instant
 */
public record CustomerVerificationRequest(
        CustomerVerificationId verificationId,
        CustomerVerificationSubject subject,
        FinancialInstitutionCode financialInstitutionCode,
        AccountBindingFingerprint accountBindingFingerprint,
        CustomerVerificationContext context,
        Instant requestedAt
) implements ValueObject {

    public CustomerVerificationRequest {
        verificationId = Objects.requireNonNull(
                verificationId,
                "verificationId is required"
        );
        subject = Objects.requireNonNull(
                subject,
                "subject is required"
        );
        financialInstitutionCode = Objects.requireNonNull(
                financialInstitutionCode,
                "financialInstitutionCode is required"
        );
        accountBindingFingerprint = Objects.requireNonNull(
                accountBindingFingerprint,
                "accountBindingFingerprint is required"
        );
        context = Objects.requireNonNull(
                context,
                "context is required"
        );
        requestedAt = Objects.requireNonNull(
                requestedAt,
                "requestedAt is required"
        );
    }

    @Override
    public String toString() {
        return "CustomerVerificationRequest[verificationId="
                + verificationId
                + ", subject=[PROTECTED]"
                + ", financialInstitutionCode="
                + financialInstitutionCode
                + ", accountBindingFingerprint=[PROTECTED]"
                + ", correlationId="
                + context.correlationId()
                + ", causationId="
                + context.causationIdOptional().orElse(null)
                + ", requestedAt="
                + requestedAt
                + "]";
    }
}
