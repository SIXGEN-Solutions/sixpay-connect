package com.sixpay.partner.domain.model;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public record AuthorizedPerimeter(Set<String> transactionTypes) {

    public AuthorizedPerimeter {
        if (transactionTypes == null || transactionTypes.isEmpty()) {
            throw new IllegalArgumentException("at least one authorized transaction type is required");
        }
        var normalized = new LinkedHashSet<String>();
        transactionTypes.forEach(value -> normalized.add(normalize(value)));
        transactionTypes = Set.copyOf(normalized);
    }

    public static AuthorizedPerimeter of(Collection<String> values) {
        return new AuthorizedPerimeter(Set.copyOf(values));
    }

    public boolean allows(String transactionType) {
        return transactionTypes.contains(normalize(transactionType));
    }

    static String normalize(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("transaction type is required");
        }
        return value.strip().toUpperCase(Locale.ROOT);
    }
}
