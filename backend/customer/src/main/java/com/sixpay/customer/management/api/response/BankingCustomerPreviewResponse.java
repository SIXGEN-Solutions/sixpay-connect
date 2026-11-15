package com.sixpay.customer.management.api.response;

import com.sixpay.customer.management.application.port.input.BankingCustomerPreviewUseCase;

import java.time.Instant;
import java.util.List;

public record BankingCustomerPreviewResponse(
        String financialInstitutionCode,
        String bankingCustomerReference,
        String customerNumber,
        String niu,
        String legalName,
        String email,
        String phoneNumber,
        List<BankingCustomerAccountPreviewResponse> accounts,
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
                preview.accounts().stream()
                        .map(BankingCustomerAccountPreviewResponse::from).toList(),
                preview.retrievedAt()
        );
    }

    public record BankingCustomerAccountPreviewResponse(
            String accountReference,
            String maskedAccountIdentifier,
            String currency,
            String accountType,
            String status,
            Instant retrievedAt
    ) {
        public static BankingCustomerAccountPreviewResponse from(
                BankingCustomerPreviewUseCase.BankingCustomerAccountPreview account
        ) {
            return new BankingCustomerAccountPreviewResponse(
                    account.accountReference(), account.maskedAccountIdentifier(),
                    account.currency(), account.accountType(), account.status(),
                    account.retrievedAt());
        }
    }
}
