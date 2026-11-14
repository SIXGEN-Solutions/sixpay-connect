package com.sixpay.payment.domain.model;

import com.sixpay.sharedkernel.domain.valueobject.Money;
import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

import java.util.ArrayList;
import java.util.Currency;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Canonical bounded Treasury allocation intention. An empty allocation set
 * denotes an unresolved Treasury allocation at Payment initiation; it never
 * denotes a zero-value payment or a fabricated beneficiary.
 */
public final class TreasuryAllocationIntent implements ValueObject {

    private static final int MAXIMUM_ALLOCATIONS = 20;

    private final List<TreasuryAllocation> allocations;
    private final Money totalAmount;

    public TreasuryAllocationIntent(
            List<TreasuryAllocation> allocations,
            Money totalAmount
    ) {
        PaymentValueObjectRules.requireNonNull(
                allocations,
                "Treasury allocations"
        );
        this.totalAmount =
                PaymentValueObjectRules.requireNonNull(
                        totalAmount,
                        "Treasury allocation total"
                );

        if (!totalAmount.isPositive()) {
            throw new IllegalArgumentException(
                    "Treasury allocation total must be positive"
            );
        }

        if (allocations.size() > MAXIMUM_ALLOCATIONS) {
            throw new IllegalArgumentException(
                    "Treasury allocation count must not exceed "
                            + MAXIMUM_ALLOCATIONS
            );
        }

        List<TreasuryAllocation> canonical =
                new ArrayList<>(allocations.size());
        Set<TreasuryBeneficiaryReference> beneficiaries =
                new HashSet<>();

        for (TreasuryAllocation allocation : allocations) {
            TreasuryAllocation validated =
                    PaymentValueObjectRules.requireNonNull(
                            allocation,
                            "Treasury allocation"
                    );

            if (!beneficiaries.add(
                    validated.beneficiaryReference()
            )) {
                throw new IllegalArgumentException(
                        "Treasury beneficiary references must be unique"
                );
            }

            if (!totalAmount.currency().equals(
                    validated.amount().currency()
            )) {
                throw new IllegalArgumentException(
                        "Treasury allocations must use the total currency"
                );
            }

            canonical.add(validated);
        }

        canonical.sort(
                (left, right) ->
                        left.beneficiaryReference()
                                .compareTo(
                                        right.beneficiaryReference()
                                )
        );

        Money calculatedTotal = Money.zero(
                totalAmount.currency()
        );

        for (TreasuryAllocation allocation : canonical) {
            calculatedTotal = calculatedTotal.add(
                    allocation.amount()
            );
        }

        // An empty list means the Treasury configuration has not been resolved
        // during initiation. It is NOT an allocation of 0 to a beneficiary.
        if (!canonical.isEmpty() && !calculatedTotal.equals(totalAmount)) {
            throw new IllegalArgumentException(
                    "Treasury allocations must sum exactly to the total"
            );
        }

        this.allocations = List.copyOf(canonical);
    }

    public List<TreasuryAllocation> allocations() {
        return allocations;
    }

    public Money totalAmount() {
        return totalAmount;
    }

    public Currency currency() {
        return totalAmount.currency();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof TreasuryAllocationIntent that)) {
            return false;
        }

        return allocations.equals(that.allocations)
                && totalAmount.equals(that.totalAmount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(allocations, totalAmount);
    }

    @Override
    public String toString() {
        return "TreasuryAllocationIntent[count="
                + allocations.size()
                + ", total=" + totalAmount + "]";
    }
}
