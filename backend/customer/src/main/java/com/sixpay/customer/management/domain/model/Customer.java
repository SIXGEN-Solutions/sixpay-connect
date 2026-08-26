package com.sixpay.customer.management.domain.model;

import com.sixpay.customer.management.domain.exception.CustomerDomainException;
import com.sixpay.sharedkernel.domain.model.AggregateRoot;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class Customer extends AggregateRoot<CustomerId> {

    private static final int MAX_INSTITUTION_CODE_LENGTH = 32;
    private static final int MAX_BANK_CUSTOMER_REFERENCE_LENGTH = 100;
    private static final int MAX_CUSTOMER_NUMBER_LENGTH = 100;
    private static final int MAX_NIU_LENGTH = 100;
    private static final int MAX_LEGAL_NAME_LENGTH = 200;
    private static final int MAX_EMAIL_LENGTH = 254;
    private static final int MAX_PHONE_LENGTH = 32;
    private static final int MAX_REASON_LENGTH = 500;

    private final String financialInstitutionCode;
    private final String bankingCustomerReference;
    private final String customerNumber;
    private final String niu;
    private String legalName;
    private String email;
    private String phoneNumber;
    private final Instant createdAt;
    private final Map<CustomerBankAccountId, CustomerBankAccount> bankAccounts;

    private CustomerStatus status;
    private String statusReason;
    private Instant updatedAt;

    private Customer(
            CustomerId id,
            String financialInstitutionCode,
            String bankingCustomerReference,
            String customerNumber,
            String niu,
            String legalName,
            String email,
            String phoneNumber,
            CustomerStatus status,
            String statusReason,
            Instant createdAt,
            Instant updatedAt,
            Collection<CustomerBankAccount> bankAccounts
    ) {
        super(id);
        this.financialInstitutionCode = requireText(
                financialInstitutionCode,
                "financialInstitutionCode",
                MAX_INSTITUTION_CODE_LENGTH
        ).toUpperCase(Locale.ROOT);
        this.bankingCustomerReference = requireText(
                bankingCustomerReference,
                "bankingCustomerReference",
                MAX_BANK_CUSTOMER_REFERENCE_LENGTH
        );
        this.customerNumber = optionalText(
                customerNumber, "customerNumber", MAX_CUSTOMER_NUMBER_LENGTH);
        this.niu = optionalText(niu, "niu", MAX_NIU_LENGTH);
        this.legalName = requireText(
                legalName, "legalName", MAX_LEGAL_NAME_LENGTH);
        this.email = normalizeEmail(email);
        this.phoneNumber = optionalText(
                phoneNumber, "phoneNumber", MAX_PHONE_LENGTH);
        this.status = Objects.requireNonNull(status, "status is required");
        this.statusReason = normalizeReason(statusReason);
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt is required");

        if (updatedAt.isBefore(createdAt)) {
            throw new CustomerDomainException(
                    "updatedAt must not precede createdAt");
        }

        this.bankAccounts = new LinkedHashMap<>();
        Objects.requireNonNull(bankAccounts, "bankAccounts are required")
                .forEach(this::restoreAccount);

        validateStatusReason();
        validateAccounts();
    }

    public static Customer create(
            CustomerId id,
            String financialInstitutionCode,
            String bankingCustomerReference,
            String customerNumber,
            String niu,
            String legalName,
            String email,
            String phoneNumber,
            CustomerBankAccount initialBankAccount,
            Instant now
    ) {
        Objects.requireNonNull(now, "now is required");
        Objects.requireNonNull(
                initialBankAccount, "initialBankAccount is required");

        return new Customer(
                id,
                financialInstitutionCode,
                bankingCustomerReference,
                customerNumber,
                niu,
                legalName,
                email,
                phoneNumber,
                CustomerStatus.ACTIVE,
                null,
                now,
                now,
                List.of(initialBankAccount.asDefault())
        );
    }

    public static Customer reconstitute(
            CustomerId id,
            String financialInstitutionCode,
            String bankingCustomerReference,
            String customerNumber,
            String niu,
            String legalName,
            String email,
            String phoneNumber,
            CustomerStatus status,
            String statusReason,
            Instant createdAt,
            Instant updatedAt,
            Collection<CustomerBankAccount> bankAccounts
    ) {
        return new Customer(
                id,
                financialInstitutionCode,
                bankingCustomerReference,
                customerNumber,
                niu,
                legalName,
                email,
                phoneNumber,
                status,
                statusReason,
                createdAt,
                updatedAt,
                bankAccounts
        );
    }

    public void updateProfile(
            String legalName,
            String email,
            String phoneNumber,
            Instant now
    ) {
        requireMutable("update customer profile");
        requireTime(now);

        this.legalName = requireText(
                legalName,
                "legalName",
                MAX_LEGAL_NAME_LENGTH
        );
        this.email = normalizeEmail(email);
        this.phoneNumber = optionalText(
                phoneNumber,
                "phoneNumber",
                MAX_PHONE_LENGTH
        );
        this.updatedAt = now;
    }

    public void suspend(String reason, Instant now) {
        requireStatus(CustomerStatus.ACTIVE, "suspend");
        transition(CustomerStatus.SUSPENDED, requireReason(reason), now);
    }

    public void reactivate(Instant now) {
        requireStatus(CustomerStatus.SUSPENDED, "reactivate");
        transition(CustomerStatus.ACTIVE, null, now);
    }

    public void close(String reason, Instant now) {
        if (status == CustomerStatus.CLOSED) {
            throw new CustomerDomainException(
                    "cannot close customer already in status CLOSED");
        }
        transition(CustomerStatus.CLOSED, requireReason(reason), now);
    }

    public void addBankAccount(
            CustomerBankAccount account,
            Instant now
    ) {
        requireMutable("add a bank account");
        Objects.requireNonNull(account, "bankAccount is required");
        requireTime(now);
        requireOwnership(account);
        requireUnique(account);

        bankAccounts.put(
                account.id(),
                bankAccounts.isEmpty()
                        ? account.asDefault()
                        : account.asNonDefault()
        );
        updatedAt = now;
    }

    public void makeDefaultBankAccount(
            CustomerBankAccountId accountId,
            Instant now
    ) {
        requireMutable("change the default bank account");
        Objects.requireNonNull(accountId, "accountId is required");
        requireTime(now);

        CustomerBankAccount selected = bankAccounts.get(accountId);
        if (selected == null) {
            throw new CustomerDomainException(
                    "bank account does not belong to customer");
        }
        if (selected.defaultAccount()) {
            return;
        }

        bankAccounts.replaceAll(
                (id, account) -> id.equals(accountId)
                        ? account.asDefault()
                        : account.asNonDefault()
        );
        updatedAt = now;
    }

    public void removeBankAccount(
            CustomerBankAccountId accountId,
            Instant now
    ) {
        requireMutable("remove a bank account");
        Objects.requireNonNull(accountId, "accountId is required");
        requireTime(now);

        if (bankAccounts.size() == 1) {
            throw new CustomerDomainException(
                    "customer must keep at least one bank account");
        }

        CustomerBankAccount removed = bankAccounts.remove(accountId);
        if (removed == null) {
            throw new CustomerDomainException(
                    "bank account does not belong to customer");
        }

        if (removed.defaultAccount()) {
            CustomerBankAccountId replacement =
                    bankAccounts.keySet().iterator().next();
            bankAccounts.computeIfPresent(
                    replacement,
                    (id, account) -> account.asDefault()
            );
        }

        updatedAt = now;
    }

    public boolean acceptsNewSubscriptions() {
        return status == CustomerStatus.ACTIVE;
    }

    public Optional<CustomerBankAccount> defaultBankAccount() {
        return bankAccounts.values().stream()
                .filter(CustomerBankAccount::defaultAccount)
                .findFirst();
    }

    private void restoreAccount(CustomerBankAccount account) {
        Objects.requireNonNull(account, "bankAccount is required");
        requireOwnership(account);
        if (bankAccounts.putIfAbsent(account.id(), account) != null) {
            throw new CustomerDomainException("duplicate bank account id");
        }
    }

    private void validateAccounts() {
        if (bankAccounts.isEmpty()) {
            throw new CustomerDomainException(
                    "customer must have at least one bank account");
        }

        long defaults = bankAccounts.values().stream()
                .filter(CustomerBankAccount::defaultAccount)
                .count();

        if (defaults != 1) {
            throw new CustomerDomainException(
                    "customer must have exactly one default bank account");
        }

        for (CustomerBankAccount account : bankAccounts.values()) {
            long references = bankAccounts.values().stream()
                    .filter(candidate ->
                            candidate.bankingAccountReference().equals(
                                    account.bankingAccountReference()))
                    .count();
            long fingerprints = bankAccounts.values().stream()
                    .filter(candidate ->
                            candidate.accountBindingFingerprint().equals(
                                    account.accountBindingFingerprint()))
                    .count();

            if (references > 1 || fingerprints > 1) {
                throw new CustomerDomainException(
                        "bank accounts must be unique by reference and fingerprint");
            }
        }
    }

    private void requireUnique(CustomerBankAccount account) {
        boolean duplicate = bankAccounts.containsKey(account.id())
                || bankAccounts.values().stream().anyMatch(existing ->
                        existing.bankingAccountReference().equals(
                                account.bankingAccountReference())
                        || existing.accountBindingFingerprint().equals(
                                account.accountBindingFingerprint()));

        if (duplicate) {
            throw new CustomerDomainException(
                    "bank account is already linked to customer");
        }
    }

    private void requireOwnership(CustomerBankAccount account) {
        if (!id().equals(account.customerId())) {
            throw new CustomerDomainException(
                    "bank account belongs to another customer");
        }
    }

    private void requireMutable(String operation) {
        if (status == CustomerStatus.CLOSED) {
            throw new CustomerDomainException(
                    "cannot " + operation + " for customer in status CLOSED");
        }
    }

    private void requireStatus(CustomerStatus expected, String operation) {
        if (status != expected) {
            throw new CustomerDomainException(
                    "cannot " + operation + " customer in status "
                            + status + "; expected " + expected);
        }
    }

    private void transition(
            CustomerStatus target,
            String reason,
            Instant now
    ) {
        requireTime(now);
        status = target;
        statusReason = normalizeReason(reason);
        validateStatusReason();
        updatedAt = now;
    }

    private void requireTime(Instant now) {
        Objects.requireNonNull(now, "now is required");
        if (now.isBefore(updatedAt)) {
            throw new CustomerDomainException(
                    "operation time must not precede updatedAt");
        }
    }

    private void validateStatusReason() {
        if ((status == CustomerStatus.SUSPENDED
                || status == CustomerStatus.CLOSED)
                && statusReason == null) {
            throw new CustomerDomainException(
                    "status reason is required for " + status);
        }
        if (status == CustomerStatus.ACTIVE && statusReason != null) {
            throw new CustomerDomainException(
                    "ACTIVE customer must not have a status reason");
        }
    }

    private static String requireText(
            String value,
            String field,
            int maxLength
    ) {
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

    private static String optionalText(
            String value,
            String field,
            int maxLength
    ) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return requireText(value, field, maxLength);
    }

    private static String normalizeEmail(String value) {
        String email = optionalText(value, "email", MAX_EMAIL_LENGTH);
        if (email == null) {
            return null;
        }
        int at = email.indexOf('@');
        if (at <= 0
                || at != email.lastIndexOf('@')
                || at == email.length() - 1) {
            throw new CustomerDomainException(
                    "email must be structurally valid");
        }
        return email.toLowerCase(Locale.ROOT);
    }

    private static String requireReason(String reason) {
        String normalized = normalizeReason(reason);
        if (normalized == null) {
            throw new CustomerDomainException("a reason is required");
        }
        return normalized;
    }

    private static String normalizeReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return null;
        }
        String normalized = reason.strip();
        if (normalized.length() > MAX_REASON_LENGTH) {
            throw new CustomerDomainException(
                    "reason must not exceed "
                            + MAX_REASON_LENGTH + " characters");
        }
        return normalized;
    }

    public String financialInstitutionCode() {
        return financialInstitutionCode;
    }

    public String bankingCustomerReference() {
        return bankingCustomerReference;
    }

    public Optional<String> customerNumber() {
        return Optional.ofNullable(customerNumber);
    }

    public Optional<String> niu() {
        return Optional.ofNullable(niu);
    }

    public String legalName() {
        return legalName;
    }

    public Optional<String> email() {
        return Optional.ofNullable(email);
    }

    public Optional<String> phoneNumber() {
        return Optional.ofNullable(phoneNumber);
    }

    public CustomerStatus status() {
        return status;
    }

    public Optional<String> statusReason() {
        return Optional.ofNullable(statusReason);
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public Collection<CustomerBankAccount> bankAccounts() {
        return List.copyOf(bankAccounts.values());
    }
}
