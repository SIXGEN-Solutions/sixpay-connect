package com.sixpay.customer.observation.domain.model;

import com.sixpay.customer.observation.domain.exception.ObservedCustomerDomainException;
import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public record ObservedCustomerInstitution(
        String financialInstitutionCode,
        Instant firstObservedAt,
        Instant lastObservedAt,
        List<ObservedAccountReference> accounts
) implements ValueObject {

    private static final Pattern CODE = Pattern.compile(
            "^[A-Z0-9][A-Z0-9._-]{0,31}$"
    );

    public ObservedCustomerInstitution {
        if (financialInstitutionCode == null) {
            throw new ObservedCustomerDomainException(
                    "financialInstitutionCode is required"
            );
        }
        financialInstitutionCode = financialInstitutionCode
                .strip().toUpperCase(Locale.ROOT);
        if (!CODE.matcher(financialInstitutionCode).matches()) {
            throw new ObservedCustomerDomainException(
                    "financialInstitutionCode has an invalid format"
            );
        }

        firstObservedAt = Objects.requireNonNull(
                firstObservedAt,
                "firstObservedAt is required"
        );
        lastObservedAt = Objects.requireNonNull(
                lastObservedAt,
                "lastObservedAt is required"
        );
        if (lastObservedAt.isBefore(firstObservedAt)) {
            throw new ObservedCustomerDomainException(
                    "institution lastObservedAt must not be before firstObservedAt"
            );
        }

        accounts = List.copyOf(
                Objects.requireNonNull(accounts, "accounts are required")
        );
        if (accounts.isEmpty() || accounts.stream().anyMatch(Objects::isNull)) {
            throw new ObservedCustomerDomainException(
                    "institution must contain non-null observed accounts"
            );
        }

        var fingerprints = new HashSet<String>();
        for (ObservedAccountReference account : accounts) {
            if (!fingerprints.add(account.accountBindingFingerprint())) {
                throw new ObservedCustomerDomainException(
                        "institution account fingerprints must be unique"
                );
            }
        }
    }

    public static ObservedCustomerInstitution of(
            String financialInstitutionCode,
            Instant firstObservedAt,
            Instant lastObservedAt,
            List<ObservedAccountReference> accounts
    ) {
        return new ObservedCustomerInstitution(
                financialInstitutionCode,
                firstObservedAt,
                lastObservedAt,
                accounts
        );
    }
}
