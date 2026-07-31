package com.sixpay.payment.domain.model;

import com.sixpay.payment.domain.model.evidence.EvidenceFingerprint;
import com.sixpay.sharedkernel.domain.valueobject.Money;
import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

import java.util.Objects;

/**
 * Immutable canonical business intention used to create one Payment.
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
        EvidenceFingerprint allocationIntentFingerprint
) implements ValueObject {

    public NewPaymentIntent {
        source = Objects.requireNonNull(source, "Payment source");
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
                debtorAccountReference.financialInstitutionCode()
        )) {
            throw new IllegalArgumentException(
                    "Debtor account institution must match Payment institution"
            );
        }
        if (!requestedAmount.equals(
                treasuryAllocationIntent.totalAmount()
        )) {
            throw new IllegalArgumentException(
                    "Treasury allocation total must equal requested amount"
            );
        }
    }
}
