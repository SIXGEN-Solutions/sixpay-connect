package com.sixpay.payment.domain.policy;

import com.sixpay.payment.domain.model.FinancialInstitutionCode;
import com.sixpay.payment.domain.model.evidence.EvidenceFingerprint;

import java.util.Objects;

public record PaymentTreasuryContext(
        FinancialInstitutionCode financialInstitutionCode,
        EvidenceFingerprint allocationIntentFingerprint
) {
    public PaymentTreasuryContext {
        financialInstitutionCode = Objects.requireNonNull(
                financialInstitutionCode,
                "Financial institution code"
        );
        allocationIntentFingerprint = Objects.requireNonNull(
                allocationIntentFingerprint,
                "Allocation intent fingerprint"
        );
    }
}
