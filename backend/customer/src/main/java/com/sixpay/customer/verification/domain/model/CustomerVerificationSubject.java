package com.sixpay.customer.verification.domain.model;

import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

import java.util.Objects;

/**
 * Business subject targeted by one Customer Verification request.
 *
 * @param identity minimal identity to compare with banking evidence
 */
public record CustomerVerificationSubject(
        CustomerIdentity identity
) implements ValueObject {

    public CustomerVerificationSubject {
        identity = Objects.requireNonNull(
                identity,
                "identity is required"
        );
    }

    public static CustomerVerificationSubject of(
            CustomerIdentity identity
    ) {
        return new CustomerVerificationSubject(identity);
    }

    @Override
    public String toString() {
        return "CustomerVerificationSubject[identity=[PROTECTED]]";
    }
}
