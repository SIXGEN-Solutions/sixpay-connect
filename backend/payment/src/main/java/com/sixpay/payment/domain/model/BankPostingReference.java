package com.sixpay.payment.domain.model;

import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

import java.util.Objects;
import java.util.Optional;

/**
 * Opaque references assigned by the bank to one posting and its legs.
 */
public final class BankPostingReference implements ValueObject {

    private final String principalPostingReference;
    private final String debitLegReference;
    private final String cutCreditLegReference;

    public BankPostingReference(
            String principalPostingReference,
            String debitLegReference,
            String cutCreditLegReference
    ) {
        this.principalPostingReference =
                PaymentValueObjectRules
                        .requirePrintableAsciiNoWhitespace(
                                principalPostingReference,
                                128,
                                "Principal posting reference"
                        );
        this.debitLegReference =
                normalizeOptionalReference(
                        debitLegReference,
                        "Debit leg reference"
                );
        this.cutCreditLegReference =
                normalizeOptionalReference(
                        cutCreditLegReference,
                        "CUT credit leg reference"
                );
    }

    public static BankPostingReference principalOnly(
            String principalPostingReference
    ) {
        return new BankPostingReference(
                principalPostingReference,
                null,
                null
        );
    }

    public String principalPostingReference() {
        return principalPostingReference;
    }

    public Optional<String> debitLegReference() {
        return Optional.ofNullable(debitLegReference);
    }

    public Optional<String> cutCreditLegReference() {
        return Optional.ofNullable(cutCreditLegReference);
    }

    private static String normalizeOptionalReference(
            String value,
            String label
    ) {
        if (value == null) {
            return null;
        }

        return PaymentValueObjectRules
                .requirePrintableAsciiNoWhitespace(
                        value,
                        128,
                        label
                );
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof BankPostingReference that)) {
            return false;
        }

        return principalPostingReference.equals(
                that.principalPostingReference
        ) && Objects.equals(
                debitLegReference,
                that.debitLegReference
        ) && Objects.equals(
                cutCreditLegReference,
                that.cutCreditLegReference
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                principalPostingReference,
                debitLegReference,
                cutCreditLegReference
        );
    }

    @Override
    public String toString() {
        return "BankPostingReference[principal="
                + principalPostingReference
                + ", debitLeg=" + debitLegReference
                + ", cutCreditLeg=" + cutCreditLegReference
                + "]";
    }
}
