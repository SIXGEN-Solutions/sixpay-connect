package com.sixpay.customer.management.application.port.output;

import java.time.Instant;
import java.util.List;
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
            Objects.requireNonNull(correlationId);
        }
    }

    /**
     * The preview exposes all authoritative GET accounts. For the legacy
     * add-account use case, account is the explicitly requested item.
     */
    record BankingCustomerProfile(
            String financialInstitutionCode,
            String customerReference,
            String customerNumber,
            String niu,
            String legalName,
            String email,
            String phoneNumber,
            BankingAccount account,
            List<BankingAccount> accounts
    ) {
        public BankingCustomerProfile {
            Objects.requireNonNull(financialInstitutionCode);
            Objects.requireNonNull(customerReference);
            Objects.requireNonNull(legalName);
            accounts = List.copyOf(Objects.requireNonNull(accounts));
        }

        /** Backward-compatible constructor used by account-linking clients. */
        public BankingCustomerProfile(
                String financialInstitutionCode, String customerReference,
                String customerNumber, String niu, String legalName,
                String email, String phoneNumber, BankingAccount account
        ) {
            this(financialInstitutionCode, customerReference, customerNumber,
                    niu, legalName, email, phoneNumber, account,
                    List.of(Objects.requireNonNull(account)));
        }
    }

    record BankingAccount(
            String accountReference,
            String accountBindingFingerprint,
            String bankingAccountAccessReference,
            String maskedAccountIdentifier,
            String currency,
            String accountType,
            String status,
            Instant retrievedAt
    ) {
        public BankingAccount {
            Objects.requireNonNull(accountReference);
            // A GET preview does not constitute account-binding evidence.
            Objects.requireNonNull(maskedAccountIdentifier);
            Objects.requireNonNull(currency);
            Objects.requireNonNull(retrievedAt);
        }

        /** Preserve the existing account-linking and test constructor. */
        public BankingAccount(
                String accountReference, String accountBindingFingerprint,
                String bankingAccountAccessReference, String maskedAccountIdentifier,
                String currency, String accountType, Instant retrievedAt
        ) {
            this(accountReference, accountBindingFingerprint,
                    bankingAccountAccessReference, maskedAccountIdentifier,
                    currency, accountType, null, retrievedAt);
        }
    }
}
