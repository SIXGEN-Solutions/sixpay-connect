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
        String legalName,
        String bankingCustomerReference,
        String customerNumber
) implements ValueObject {

    private static final int MIN_LEGAL_NAME_LENGTH = 2;
    private static final int MAX_LEGAL_NAME_LENGTH = 200;

    public CustomerIdentity {
        bankingCustomerReference = normalize(bankingCustomerReference);
        customerNumber = normalize(customerNumber);
        if (niu == null && bankingCustomerReference == null && customerNumber == null) {
            throw new CustomerVerificationDomainException(
                    "At least one banking customer identifier is required");
        }

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
        return new CustomerIdentity(niu, legalName, null, null);
    }

    public static CustomerIdentity of(
            CustomerNiu niu, String legalName,
            String bankingCustomerReference, String customerNumber) {
        return new CustomerIdentity(niu, legalName, bankingCustomerReference, customerNumber);
    }

    public CustomerIdentity(CustomerNiu niu, String legalName) {
        this(niu, legalName, null, null);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    @Override
    public String toString() {
        return "CustomerIdentity[niu=[PROTECTED], legalName=[PROTECTED]]";
    }
}
