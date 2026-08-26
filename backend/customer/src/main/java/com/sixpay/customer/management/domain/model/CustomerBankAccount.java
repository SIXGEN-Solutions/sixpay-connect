package com.sixpay.customer.management.domain.model;

import com.sixpay.customer.management.domain.exception.CustomerDomainException;

import java.time.Instant;
import java.util.Currency;
import java.util.Locale;
import java.util.Objects;

public final class CustomerBankAccount {

    private static final int MAX_REFERENCE_LENGTH = 100;
    private static final int MAX_FINGERPRINT_LENGTH = 128;
    private static final int MAX_MASKED_IDENTIFIER_LENGTH = 100;
    private static final int MAX_ACCOUNT_TYPE_LENGTH = 40;

    private final CustomerBankAccountId id;
    private final CustomerId customerId;
    private final String bankingAccountReference;
    private final String accountBindingFingerprint;
    private final String maskedAccountIdentifier;
    private final String currency;
    private final String accountType;
    private final boolean defaultAccount;
    private final Instant verifiedAt;

    private CustomerBankAccount(
            CustomerBankAccountId id,
            CustomerId customerId,
            String bankingAccountReference,
            String accountBindingFingerprint,
            String maskedAccountIdentifier,
            String currency,
            String accountType,
            boolean defaultAccount,
            Instant verifiedAt
    ) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.customerId = Objects.requireNonNull(customerId, "customerId is required");
        this.bankingAccountReference = requireText(
                bankingAccountReference, "bankingAccountReference", MAX_REFERENCE_LENGTH);
        this.accountBindingFingerprint = requireText(
                accountBindingFingerprint, "accountBindingFingerprint", MAX_FINGERPRINT_LENGTH);
        this.maskedAccountIdentifier = requireText(
                maskedAccountIdentifier, "maskedAccountIdentifier", MAX_MASKED_IDENTIFIER_LENGTH);
        this.currency = normalizeCurrency(currency);
        this.accountType = normalizeOptionalText(
                accountType, "accountType", MAX_ACCOUNT_TYPE_LENGTH);
        this.defaultAccount = defaultAccount;
        this.verifiedAt = Objects.requireNonNull(verifiedAt, "verifiedAt is required");
    }

    public static CustomerBankAccount create(
            CustomerBankAccountId id,
            CustomerId customerId,
            String bankingAccountReference,
            String accountBindingFingerprint,
            String maskedAccountIdentifier,
            String currency,
            String accountType,
            Instant verifiedAt
    ) {
        return new CustomerBankAccount(
                id,
                customerId,
                bankingAccountReference,
                accountBindingFingerprint,
                maskedAccountIdentifier,
                currency,
                accountType,
                false,
                verifiedAt
        );
    }

    public static CustomerBankAccount reconstitute(
            CustomerBankAccountId id,
            CustomerId customerId,
            String bankingAccountReference,
            String accountBindingFingerprint,
            String maskedAccountIdentifier,
            String currency,
            String accountType,
            boolean defaultAccount,
            Instant verifiedAt
    ) {
        return new CustomerBankAccount(
                id,
                customerId,
                bankingAccountReference,
                accountBindingFingerprint,
                maskedAccountIdentifier,
                currency,
                accountType,
                defaultAccount,
                verifiedAt
        );
    }

    CustomerBankAccount asDefault() {
        return defaultAccount ? this : copy(true);
    }

    CustomerBankAccount asNonDefault() {
        return defaultAccount ? copy(false) : this;
    }

    private CustomerBankAccount copy(boolean targetDefault) {
        return new CustomerBankAccount(
                id,
                customerId,
                bankingAccountReference,
                accountBindingFingerprint,
                maskedAccountIdentifier,
                currency,
                accountType,
                targetDefault,
                verifiedAt
        );
    }

    private static String requireText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new CustomerDomainException(field + " is required");
        }
        String normalized = value.strip();
        if (normalized.length() > maxLength) {
            throw new CustomerDomainException(
                    field + " must not exceed " + maxLength + " characters");
        }
        return normalized;
    }

    private static String normalizeOptionalText(
            String value,
            String field,
            int maxLength
    ) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.strip();
        if (normalized.length() > maxLength) {
            throw new CustomerDomainException(
                    field + " must not exceed " + maxLength + " characters");
        }
        return normalized;
    }

    private static String normalizeCurrency(String value) {
        String normalized = requireText(value, "currency", 3)
                .toUpperCase(Locale.ROOT);
        try {
            return Currency.getInstance(normalized).getCurrencyCode();
        } catch (IllegalArgumentException exception) {
            throw new CustomerDomainException(
                    "currency must be a valid ISO-4217 code");
        }
    }

    public CustomerBankAccountId id() {
        return id;
    }

    public CustomerId customerId() {
        return customerId;
    }

    public String bankingAccountReference() {
        return bankingAccountReference;
    }

    public String accountBindingFingerprint() {
        return accountBindingFingerprint;
    }

    public String maskedAccountIdentifier() {
        return maskedAccountIdentifier;
    }

    public String currency() {
        return currency;
    }

    public String accountType() {
        return accountType;
    }

    public boolean defaultAccount() {
        return defaultAccount;
    }

    public Instant verifiedAt() {
        return verifiedAt;
    }
}
