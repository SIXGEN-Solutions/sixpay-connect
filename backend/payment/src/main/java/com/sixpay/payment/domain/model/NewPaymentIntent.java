package com.sixpay.payment.domain.model;

import com.sixpay.payment.domain.model.evidence.EvidenceFingerprint;
import com.sixpay.sharedkernel.domain.valueobject.Money;
import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

import java.util.Objects;

/**
 * Immutable canonical business intention used to create one Payment.
 *
 * @param source payment source
 * @param externalPaymentReference external business payment reference
 * @param externalSubscriptionReference external subscription reference
 * @param requestIdentity technical request identity and idempotency data
 * @param financialInstitutionCode target financial institution
 * @param debtorAccountReference protected debtor account reference
 * @param requestedAmount total requested amount
 * @param treasuryAllocationIntent beneficiary allocation intention
 * @param allocationIntentFingerprint canonical allocation fingerprint
 * @param initiationContext non-secret TresorPay initiation context
 */
public record NewPaymentIntent(
        PaymentSource source,
        ExternalPaymentReference externalPaymentReference,
        ExternalSubscriptionReference externalSubscriptionReference,
        PaymentRequestIdentity requestIdentity,
        FinancialInstitutionCode financialInstitutionCode,
        DebtorAccountReference debtorAccountReference,
        Money requestedAmount,
        TreasuryAllocationIntent treasuryAllocationIntent,
        EvidenceFingerprint allocationIntentFingerprint,
        PaymentInitiationContext initiationContext
) implements ValueObject {

    /**
     * Backward-compatible constructor for existing internal callers and tests
     * that do not yet provide a TresorPay initiation context.
     */
    public NewPaymentIntent(
            PaymentSource source,
            ExternalPaymentReference externalPaymentReference,
            ExternalSubscriptionReference externalSubscriptionReference,
            PaymentRequestIdentity requestIdentity,
            FinancialInstitutionCode financialInstitutionCode,
            DebtorAccountReference debtorAccountReference,
            Money requestedAmount,
            TreasuryAllocationIntent treasuryAllocationIntent,
            EvidenceFingerprint allocationIntentFingerprint
    ) {
        this(
                source,
                externalPaymentReference,
                externalSubscriptionReference,
                requestIdentity,
                financialInstitutionCode,
                debtorAccountReference,
                requestedAmount,
                treasuryAllocationIntent,
                allocationIntentFingerprint,
                null
        );
    }

    public NewPaymentIntent {
        source = Objects.requireNonNull(
                source,
                "Payment source"
        );

        externalPaymentReference = Objects.requireNonNull(
                externalPaymentReference,
                "External Payment reference"
        );

        externalSubscriptionReference = Objects.requireNonNull(
                externalSubscriptionReference,
                "External Subscription reference"
        );

        requestIdentity = Objects.requireNonNull(
                requestIdentity,
                "Payment request identity"
        );

        financialInstitutionCode = Objects.requireNonNull(
                financialInstitutionCode,
                "Financial institution code"
        );

        debtorAccountReference = Objects.requireNonNull(
                debtorAccountReference,
                "Debtor account reference"
        );

        requestedAmount = Objects.requireNonNull(
                requestedAmount,
                "Requested amount"
        );

        treasuryAllocationIntent = Objects.requireNonNull(
                treasuryAllocationIntent,
                "Treasury allocation intent"
        );

        allocationIntentFingerprint = Objects.requireNonNull(
                allocationIntentFingerprint,
                "Allocation intent fingerprint"
        );

        if (!requestedAmount.isPositive()) {
            throw new IllegalArgumentException(
                    "Requested amount must be positive"
            );
        }

        if (!financialInstitutionCode.equals(
                debtorAccountReference
                        .financialInstitutionCode()
        )) {
            throw new IllegalArgumentException(
                    "Debtor account institution must match "
                            + "Payment institution"
            );
        }

        if (!requestedAmount.equals(
                treasuryAllocationIntent.totalAmount()
        )) {
            throw new IllegalArgumentException(
                    "Treasury allocation total must equal "
                            + "requested amount"
            );
        }
    }
}