package com.sixpay.customer.management.api.response;

import com.sixpay.customer.management.application.port.input.BankingCustomerPreviewUseCase;

import java.time.Instant;

public record BankingCustomerPreviewResponse(
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
    public static BankingCustomerPreviewResponse from(
            BankingCustomerPreviewUseCase.BankingCustomerPreview preview
    ) {
        return new BankingCustomerPreviewResponse(
                preview.financialInstitutionCode(),
                preview.bankingCustomerReference(),
                preview.customerNumber(),
                preview.niu(),
                preview.legalName(),
                preview.email(),
                preview.phoneNumber(),
                preview.accountReference(),
                preview.maskedAccountIdentifier(),
                preview.currency(),
                preview.accountType(),
                preview.retrievedAt()
        );
    }
}
