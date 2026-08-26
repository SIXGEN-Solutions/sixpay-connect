package com.sixpay.customer.observation.infrastructure.query.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Institution row and its safe technical account rows.
 */
public record ObservedInstitutionRow(
        UUID observedInstitutionId,
        String financialInstitutionCode,
        Instant firstObservedAt,
        Instant lastObservedAt,
        List<AccountRow> accounts
) {

    public ObservedInstitutionRow {
        observedInstitutionId = Objects.requireNonNull(
                observedInstitutionId,
                "observedInstitutionId is required"
        );
        financialInstitutionCode = Objects.requireNonNull(
                financialInstitutionCode,
                "financialInstitutionCode is required"
        );
        firstObservedAt = Objects.requireNonNull(
                firstObservedAt,
                "firstObservedAt is required"
        );
        lastObservedAt = Objects.requireNonNull(
                lastObservedAt,
                "lastObservedAt is required"
        );
        accounts = List.copyOf(
                Objects.requireNonNull(
                        accounts,
                        "accounts is required"
                )
        );
    }

    /**
     * Account query row. The binding fingerprint is deliberately absent.
     */
    public record AccountRow(
            UUID observedAccountId,
            String maskedValue
    ) {

        public AccountRow {
            observedAccountId = Objects.requireNonNull(
                    observedAccountId,
                    "observedAccountId is required"
            );
            maskedValue = Objects.requireNonNull(
                    maskedValue,
                    "maskedValue is required"
            );
        }
    }
}
