package com.sixpay.customer.management.application.port.output;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public interface BankingCustomerLookupPort {

    BankingCustomerProfile lookup(BankingCustomerLookupQuery query);

    record BankingCustomerLookupQuery(
            String financialInstitutionCode,
            String niu,
            String customerNumber,
            String accountReference,
            String correlationId
    ) {
        public BankingCustomerLookupQuery {
            Objects.requireNonNull(financialInstitutionCode);
            Objects.requireNonNull(accountReference);
            Objects.requireNonNull(correlationId);
        }
    }

    record BankingCustomerProfile(
            String financialInstitutionCode,
            String customerReference,
            String customerNumber,
            String niu,
            String legalName,
            String email,
            String phoneNumber,
            BankingAccount account
    ) {
        public BankingCustomerProfile {
            Objects.requireNonNull(financialInstitutionCode);
            Objects.requireNonNull(customerReference);
            Objects.requireNonNull(niu);
            Objects.requireNonNull(legalName);
            Objects.requireNonNull(account);
        }
    }

    record BankingAccount(
            String accountReference,
            String accountBindingFingerprint,
            String bankingAccountAccessReference,
            String maskedAccountIdentifier,
            String currency,
            String accountType,
            Instant retrievedAt
    ) {
        public BankingAccount {
            Objects.requireNonNull(accountReference);
            Objects.requireNonNull(accountBindingFingerprint);
            Objects.requireNonNull(bankingAccountAccessReference);
            Objects.requireNonNull(maskedAccountIdentifier);
            Objects.requireNonNull(currency);
            Objects.requireNonNull(retrievedAt);
        }
    }
}
