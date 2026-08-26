package com.sixpay.customer.management.api.response;

import com.sixpay.customer.management.domain.model.CustomerBankAccount;

import java.time.Instant;
import java.util.UUID;

public record CustomerBankAccountResponse(
        UUID id,
        String bankingAccountReference,
        String accountBindingFingerprint,
        String maskedAccountIdentifier,
        String currency,
        String accountType,
        boolean defaultAccount,
        Instant verifiedAt
) {
    public static CustomerBankAccountResponse from(
            CustomerBankAccount account
    ) {
        return new CustomerBankAccountResponse(
                account.id().value(),
                account.bankingAccountReference(),
                account.accountBindingFingerprint(),
                account.maskedAccountIdentifier(),
                account.currency(),
                account.accountType(),
                account.defaultAccount(),
                account.verifiedAt()
        );
    }
}
