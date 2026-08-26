package com.sixpay.accounting.domain.policy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

public record AccountingSelectionWindow(
        LocalDate businessDate,
        Instant fromInclusive,
        Instant toExclusive
) {
    public AccountingSelectionWindow {
        businessDate = Objects.requireNonNull(businessDate, "businessDate");
        fromInclusive = Objects.requireNonNull(fromInclusive, "fromInclusive");
        toExclusive = Objects.requireNonNull(toExclusive, "toExclusive");
        if (!fromInclusive.isBefore(toExclusive)) {
            throw new IllegalArgumentException("Selection window must be non-empty");
        }
    }

    public boolean contains(Instant instant) {
        return !instant.isBefore(fromInclusive) && instant.isBefore(toExclusive);
    }
}
