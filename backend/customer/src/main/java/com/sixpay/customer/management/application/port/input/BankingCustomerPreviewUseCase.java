package com.sixpay.customer.management.application.port.input;

import java.time.Instant;

public interface BankingCustomerPreviewUseCase {

    BankingCustomerPreview preview(BankingCustomerPreviewQuery query);

    record BankingCustomerPreviewQuery(
            String financialInstitutionCode,
            String niu,
            String customerNumber,
            String accountReference,
            String correlationId
    ) {
    }

    record BankingCustomerPreview(
            String financialInstitutionCode,
            String bankingCustomerReference,
            String customerNumber,
            String niu,
            String legalName,
            String email,
            String phoneNumber,
            String accountReference,
            String maskedAccountIdentifier,
            String currency,
            String accountType,
            Instant retrievedAt
    ) {
    }
}
