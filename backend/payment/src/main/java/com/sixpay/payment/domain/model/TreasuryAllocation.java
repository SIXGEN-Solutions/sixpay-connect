package com.sixpay.payment.domain.model;

import com.sixpay.sharedkernel.domain.valueobject.Money;
import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

/**
 * One positive Treasury allocation.
 *
 * @param beneficiaryReference beneficiary classification
 * @param amount allocated amount
 */
public record TreasuryAllocation(
        TreasuryBeneficiaryReference beneficiaryReference,
        Money amount
) implements ValueObject {

    public TreasuryAllocation {
        beneficiaryReference =
                PaymentValueObjectRules.requireNonNull(
                        beneficiaryReference,
                        "Treasury beneficiary reference"
                );
        amount = PaymentValueObjectRules.requireNonNull(
                amount,
                "Treasury allocation amount"
        );

        if (!amount.isPositive()) {
            throw new IllegalArgumentException(
                    "Treasury allocation amount must be positive"
            );
        }
    }
}
