package com.sixpay.payment.domain.model;

import com.sixpay.sharedkernel.domain.valueobject.Money;
import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

import java.util.Objects;

/**
 * Immutable logical context to which one bank confirmation challenge is bound.
 *
 * <p>The fields correspond to the approved Core Banking create/send binding:
 * Payment reference, opaque customer reference, opaque debtor-account
 * reference and financial amount. No OTP or authentication secret belongs
 * to this value object.</p>
 */
public record ConfirmationChallengeBinding(
        PublicPaymentReference paymentReference,
        String customerReference,
        String debtorAccountReference,
        Money amount
) implements ValueObject {

    public ConfirmationChallengeBinding {
        paymentReference = Objects.requireNonNull(
                paymentReference,
                "Payment reference"
        );
        customerReference = PaymentValueObjectRules.requireOpaque(
                customerReference,
                1,
                100,
                "Confirmation customer reference"
        );
        debtorAccountReference = PaymentValueObjectRules.requireOpaque(
                debtorAccountReference,
                1,
                100,
                "Confirmation debtor-account reference"
        );
        amount = Objects.requireNonNull(amount, "Confirmation amount");

        if (!amount.isPositive()) {
            throw new IllegalArgumentException(
                    "Confirmation amount must be positive"
            );
        }
    }

    @Override
    public String toString() {
        return "ConfirmationChallengeBinding[paymentReference="
                + paymentReference
                + ", customerReference=<opaque>"
                + ", debtorAccountReference=<opaque>"
                + ", amount="
                + amount
                + "]";
    }
}
