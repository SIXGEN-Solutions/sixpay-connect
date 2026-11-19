package com.sixpay.customer.management.application.port.input;

import java.time.Instant;
import java.util.List;

public interface BankingCustomerPreviewUseCase {

    BankingCustomerPreview preview(BankingCustomerPreviewQuery query);

    record BankingCustomerPreviewQuery(
            String financialInstitutionCode,
            String customerNumber,
            String correlationId
    ) { }

    record BankingCustomerAccountPreview(
            String accountReference,
            String maskedAccountIdentifier,
            String currency,
            String accountType,
            String status,
            Instant retrievedAt
    ) { }

    record BankingCustomerPreview(
            String financialInstitutionCode,
            String bankingCustomerReference,
            String customerNumber,
            String niu,
            String legalName,
            String email,
            String phoneNumber,
            List<BankingCustomerAccountPreview> accounts,
            Instant retrievedAt
    ) { }
}
