package com.sixpay.customer.verification.domain.model;

import com.sixpay.customer.verification.domain.exception.CustomerVerificationDomainException;
import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

import java.util.Objects;

/**
 * Minimal normalized identity required for Customer Verification.
 *
 * @param niu normalized customer NIU
 * @param legalName normalized legal name used for identity comparison
 */
public record CustomerIdentity(
        CustomerNiu niu,
        String legalName
) implements ValueObject {

    private static final int MIN_LEGAL_NAME_LENGTH = 2;
    private static final int MAX_LEGAL_NAME_LENGTH = 200;

    public CustomerIdentity {
        niu = Objects.requireNonNull(niu, "niu is required");

        if (legalName == null) {
            throw new CustomerVerificationDomainException(
                    "Customer legal name is required"
            );
        }

        legalName = legalName
                .strip()
                .replaceAll("\\s+", " ");

        if (legalName.length() < MIN_LEGAL_NAME_LENGTH) {
            throw new CustomerVerificationDomainException(
                    "Customer legal name must contain at least "
                            + MIN_LEGAL_NAME_LENGTH + " characters"
            );
        }
        if (legalName.length() > MAX_LEGAL_NAME_LENGTH) {
            throw new CustomerVerificationDomainException(
                    "Customer legal name must not exceed "
                            + MAX_LEGAL_NAME_LENGTH + " characters"
            );
        }
        if (legalName.chars().anyMatch(Character::isISOControl)) {
            throw new CustomerVerificationDomainException(
                    "Customer legal name must not contain control characters"
            );
        }
    }

    public static CustomerIdentity of(
            CustomerNiu niu,
            String legalName
    ) {
        return new CustomerIdentity(niu, legalName);
    }

    @Override
    public String toString() {
        return "CustomerIdentity[niu=[PROTECTED], legalName=[PROTECTED]]";
    }
}
