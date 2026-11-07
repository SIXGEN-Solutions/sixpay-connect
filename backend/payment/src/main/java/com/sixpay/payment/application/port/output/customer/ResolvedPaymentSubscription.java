package com.sixpay.payment.application.port.output.customer;

import java.util.Objects;
import java.util.UUID;

/** Payment-owned projection of the CustomerSubscription resolved for one Partner. */
public record ResolvedPaymentSubscription(
        UUID subscriptionId,
        UUID partnerId,
        String financialInstitutionCode,
        String bankingCustomerReference,
        String customerNumber,
        String niu,
        String legalName,
        String bankingAccountReference,
        String accountBindingFingerprint,
        String maskedAccountIdentifier
) {
    public ResolvedPaymentSubscription {
        Objects.requireNonNull(subscriptionId, "subscriptionId is required");
        Objects.requireNonNull(partnerId, "partnerId is required");
        if (financialInstitutionCode == null || financialInstitutionCode.isBlank()) {
            throw new IllegalArgumentException("financialInstitutionCode is required");
        }
        if ((bankingCustomerReference == null || bankingCustomerReference.isBlank())
                && (customerNumber == null || customerNumber.isBlank())
                && (niu == null || niu.isBlank())
                && (bankingAccountReference == null || bankingAccountReference.isBlank())) {
            throw new IllegalArgumentException("At least one banking identifier is required");
        }
    }

    @Override
    public String toString() {
        return "ResolvedPaymentSubscription[subscriptionId=" + subscriptionId
                + ", partnerId=" + partnerId + ", bankingData=[PROTECTED]]";
    }
}
