package com.sixpay.customer.management.application.port.input;

import java.util.Objects;

public record EnrollCustomerCommand(
        String financialInstitutionCode,
        String niu,
        String customerNumber,
        String accountReference,
        String correlationId
) {
    public EnrollCustomerCommand {
        financialInstitutionCode = require(financialInstitutionCode, "financialInstitutionCode");
        customerNumber = require(customerNumber, "customerNumber");
        niu = normalize(niu);
        accountReference = require(accountReference, "accountReference");
        correlationId = require(correlationId, "correlationId");
    }

    private static String require(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.strip();
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
