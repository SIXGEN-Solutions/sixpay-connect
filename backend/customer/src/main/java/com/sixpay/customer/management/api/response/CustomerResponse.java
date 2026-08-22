package com.sixpay.customer.management.api.response;

import com.sixpay.customer.management.domain.model.Customer;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CustomerResponse(
        UUID id,
        String financialInstitutionCode,
        String bankingCustomerReference,
        String customerNumber,
        String niu,
        String legalName,
        String email,
        String phoneNumber,
        String status,
        String statusReason,
        Instant createdAt,
        Instant updatedAt,
        List<CustomerBankAccountResponse> bankAccounts
) {
    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(
                customer.id().value(),
                customer.financialInstitutionCode(),
                customer.bankingCustomerReference(),
                customer.customerNumber().orElse(null),
                customer.niu().orElse(null),
                customer.legalName(),
                customer.email().orElse(null),
                customer.phoneNumber().orElse(null),
                customer.status().name(),
                customer.statusReason().orElse(null),
                customer.createdAt(),
                customer.updatedAt(),
                customer.bankAccounts().stream()
                        .map(CustomerBankAccountResponse::from)
                        .toList()
        );
    }
}
