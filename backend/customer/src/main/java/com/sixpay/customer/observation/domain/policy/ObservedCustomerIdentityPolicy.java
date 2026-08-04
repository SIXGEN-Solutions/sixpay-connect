package com.sixpay.customer.observation.domain.policy;

import com.sixpay.customer.observation.domain.exception.ObservedCustomerDomainException;
import com.sixpay.customer.observation.domain.model.ObservedCustomerIdentity;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;

public final class ObservedCustomerIdentityPolicy {

    private ObservedCustomerIdentityPolicy() {
    }

    public static void requireCompatible(
            ObservedCustomerIdentity current,
            ObservedCustomerIdentity candidate
    ) {
        Objects.requireNonNull(current, "current identity is required");
        Objects.requireNonNull(candidate, "candidate identity is required");

        if (!current.normalizedNiu().equals(candidate.normalizedNiu())) {
            throw new ObservedCustomerDomainException(
                    "Observed Customer identities must have "
                            + "the same normalized NIU"
            );
        }

        if (!canonicalName(current.legalName()).equals(
                canonicalName(candidate.legalName())
        )) {
            throw new ObservedCustomerDomainException(
                    "Conflicting legal names observed for "
                            + "the same normalized NIU"
            );
        }
    }

    public static boolean compatible(
            ObservedCustomerIdentity current,
            ObservedCustomerIdentity candidate
    ) {
        try {
            requireCompatible(current, candidate);
            return true;
        } catch (ObservedCustomerDomainException exception) {
            return false;
        }
    }

    private static String canonicalName(String value) {
        String decomposed = Normalizer.normalize(
                value,
                Normalizer.Form.NFD
        );

        return decomposed
                .replaceAll("\\p{M}+", "")
                .replaceAll("[^\\p{Alnum}]+", " ")
                .strip()
                .replaceAll("\\s+", " ")
                .toUpperCase(Locale.ROOT);
    }
}
