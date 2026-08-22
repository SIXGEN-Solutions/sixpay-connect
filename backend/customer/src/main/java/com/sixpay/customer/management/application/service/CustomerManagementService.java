package com.sixpay.customer.management.application.service;

import com.sixpay.customer.management.application.port.input.AddBankAccountCommand;
import com.sixpay.customer.management.application.port.input.CustomerManagementUseCase;
import com.sixpay.customer.management.application.port.input.CustomerQueryUseCase;
import com.sixpay.customer.management.application.port.output.CustomerEnrollmentIdGenerator;
import com.sixpay.customer.management.domain.exception.CustomerDomainException;
import com.sixpay.customer.management.domain.model.Customer;
import com.sixpay.customer.management.domain.model.CustomerBankAccount;
import com.sixpay.customer.management.domain.model.CustomerBankAccountId;
import com.sixpay.customer.management.domain.model.CustomerId;
import com.sixpay.customer.management.domain.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;

@Service
@Transactional
public final class CustomerManagementService
        implements CustomerManagementUseCase, CustomerQueryUseCase {

    private final CustomerRepository repository;
    private final CustomerEnrollmentIdGenerator idGenerator;

    public CustomerManagementService(
            CustomerRepository repository,
            CustomerEnrollmentIdGenerator idGenerator
    ) {
        this.repository = Objects.requireNonNull(repository);
        this.idGenerator = Objects.requireNonNull(idGenerator);
    }

    @Override
    @Transactional(readOnly = true)
    public Customer findById(CustomerId customerId) {
        return repository.findById(customerId)
                .orElseThrow(() -> new CustomerDomainException(
                        "customer not found: " + customerId
                ));
    }

    @Override
    public Customer updateProfile(
            CustomerId customerId,
            String legalName,
            String email,
            String phoneNumber,
            Instant now
    ) {
        Customer customer = findById(customerId);
        customer.updateProfile(legalName, email, phoneNumber, now);
        return repository.save(customer);
    }

    @Override
    public Customer suspend(
            CustomerId customerId,
            String reason,
            Instant now
    ) {
        Customer customer = findById(customerId);
        customer.suspend(reason, now);
        return repository.save(customer);
    }

    @Override
    public Customer reactivate(
            CustomerId customerId,
            Instant now
    ) {
        Customer customer = findById(customerId);
        customer.reactivate(now);
        return repository.save(customer);
    }

    @Override
    public Customer close(
            CustomerId customerId,
            String reason,
            Instant now
    ) {
        Customer customer = findById(customerId);
        customer.close(reason, now);
        return repository.save(customer);
    }

    @Override
    public Customer addBankAccount(
            CustomerId customerId,
            AddBankAccountCommand command,
            Instant now
    ) {
        Customer customer = findById(customerId);

        customer.addBankAccount(
                CustomerBankAccount.create(
                        new CustomerBankAccountId(
                                idGenerator.nextId()
                        ),
                        customerId,
                        command.bankingAccountReference(),
                        command.accountBindingFingerprint(),
                        command.maskedAccountIdentifier(),
                        command.currency(),
                        command.accountType(),
                        command.verifiedAt()
                ),
                now
        );

        return repository.save(customer);
    }

    @Override
    public Customer makeDefaultBankAccount(
            CustomerId customerId,
            CustomerBankAccountId accountId,
            Instant now
    ) {
        Customer customer = findById(customerId);
        customer.makeDefaultBankAccount(accountId, now);
        return repository.save(customer);
    }

    @Override
    public Customer removeBankAccount(
            CustomerId customerId,
            CustomerBankAccountId accountId,
            Instant now
    ) {
        Customer customer = findById(customerId);
        customer.removeBankAccount(accountId, now);
        return repository.save(customer);
    }
}
