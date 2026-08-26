package com.sixpay.customer.management.domain.repository;

import com.sixpay.customer.management.domain.model.CustomerStatus;

public record CustomerSearchCriteria(
        String niu,
        String legalName,
        CustomerStatus status,
        String financialInstitutionCode,
        int page,
        int size
) {
    public CustomerSearchCriteria {
        if (page < 0) {
            throw new IllegalArgumentException(
                    "page must be greater than or equal to 0"
            );
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "size must be between 1 and 100"
            );
        }
        niu = normalize(niu);
        legalName = normalize(legalName);
        financialInstitutionCode =
                normalize(financialInstitutionCode);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank()
                ? null
                : value.strip();
    }
}
