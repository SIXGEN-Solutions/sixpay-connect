package com.sixpay.payment.domain.model;

import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

import java.util.Objects;

/**
 * Protected reference to one resolved Treasury/CUT configuration.
 */
public final class TreasuryAccountReference implements ValueObject {

    private final FinancialInstitutionCode financialInstitutionCode;
    private final String treasuryConfigurationId;
    private final String accountToken;
    private final String maskedDisplay;
    private final String configurationVersion;

    public TreasuryAccountReference(
            FinancialInstitutionCode financialInstitutionCode,
            String treasuryConfigurationId,
            String accountToken,
            String maskedDisplay,
            String configurationVersion
    ) {
        this.financialInstitutionCode =
                PaymentValueObjectRules.requireNonNull(
                        financialInstitutionCode,
                        "Financial institution code"
                );
        this.treasuryConfigurationId =
                PaymentValueObjectRules.requireOpaque(
                        treasuryConfigurationId,
                        1,
                        128,
                        "Treasury configuration ID"
                );
        this.accountToken =
                PaymentValueObjectRules.requireOpaque(
                        accountToken,
                        1,
                        256,
                        "Treasury account token"
                );
        this.maskedDisplay =
                PaymentValueObjectRules.requireMaskedDisplay(
                        maskedDisplay,
                        this.accountToken,
                        "Masked Treasury account display"
                );
        this.configurationVersion =
                PaymentValueObjectRules.requireOpaque(
                        configurationVersion,
                        1,
                        128,
                        "Treasury configuration version"
                );
    }

    public FinancialInstitutionCode financialInstitutionCode() {
        return financialInstitutionCode;
    }

    public String treasuryConfigurationId() {
        return treasuryConfigurationId;
    }

    public String accountToken() {
        return accountToken;
    }

    public String maskedDisplay() {
        return maskedDisplay;
    }

    public String configurationVersion() {
        return configurationVersion;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof TreasuryAccountReference that)) {
            return false;
        }

        return financialInstitutionCode.equals(
                that.financialInstitutionCode
        ) && treasuryConfigurationId.equals(
                that.treasuryConfigurationId
        ) && configurationVersion.equals(
                that.configurationVersion
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                financialInstitutionCode,
                treasuryConfigurationId,
                configurationVersion
        );
    }

    @Override
    public String toString() {
        return maskedDisplay;
    }
}
