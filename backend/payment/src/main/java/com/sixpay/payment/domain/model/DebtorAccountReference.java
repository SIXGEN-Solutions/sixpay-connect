package com.sixpay.payment.domain.model;

import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Protected debtor-account reference.
 *
 * <p>The clear account number never enters this object.</p>
 */
public final class DebtorAccountReference implements ValueObject {

    private static final Pattern FINGERPRINT_FORMAT =
            Pattern.compile("^v1:[0-9a-f]{64}$");

    private final FinancialInstitutionCode financialInstitutionCode;
    private final String integrationAccountToken;
    private final String maskedDisplay;
    private final String bindingFingerprint;

    public DebtorAccountReference(
            FinancialInstitutionCode financialInstitutionCode,
            String integrationAccountToken,
            String maskedDisplay,
            String bindingFingerprint
    ) {
        this.financialInstitutionCode =
                PaymentValueObjectRules.requireNonNull(
                        financialInstitutionCode,
                        "Financial institution code"
                );
        this.integrationAccountToken =
                PaymentValueObjectRules.requireOpaque(
                        integrationAccountToken,
                        1,
                        256,
                        "Integration account token"
                );
        this.maskedDisplay =
                PaymentValueObjectRules.requireMaskedDisplay(
                        maskedDisplay,
                        this.integrationAccountToken,
                        "Masked debtor account display"
                );
        this.bindingFingerprint =
                PaymentValueObjectRules.requirePattern(
                        bindingFingerprint,
                        FINGERPRINT_FORMAT,
                        67,
                        67,
                        "Account binding fingerprint"
                );
    }

    public FinancialInstitutionCode financialInstitutionCode() {
        return financialInstitutionCode;
    }

    public String integrationAccountToken() {
        return integrationAccountToken;
    }

    public String maskedDisplay() {
        return maskedDisplay;
    }

    public String bindingFingerprint() {
        return bindingFingerprint;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof DebtorAccountReference that)) {
            return false;
        }

        return financialInstitutionCode.equals(
                that.financialInstitutionCode
        ) && integrationAccountToken.equals(
                that.integrationAccountToken
        ) && bindingFingerprint.equals(
                that.bindingFingerprint
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                financialInstitutionCode,
                integrationAccountToken,
                bindingFingerprint
        );
    }

    @Override
    public String toString() {
        return maskedDisplay;
    }
}
