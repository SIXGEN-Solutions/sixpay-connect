package com.sixpay.customer.management.application.service;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.customer.management.application.port.input.AddBankAccountCommand;
import com.sixpay.customer.management.application.port.input.CustomerManagementUseCase;
import com.sixpay.customer.management.application.port.input.CustomerQueryUseCase;
import com.sixpay.customer.management.application.port.output.BankingCustomerLookupPort;
import com.sixpay.customer.management.application.port.output.CustomerEnrollmentIdGenerator;
import com.sixpay.customer.management.domain.exception.CustomerDomainException;
import com.sixpay.customer.management.domain.model.Customer;
import com.sixpay.customer.management.domain.model.CustomerBankAccount;
import com.sixpay.customer.management.domain.model.CustomerBankAccountId;
import com.sixpay.customer.management.domain.model.CustomerId;
import com.sixpay.customer.management.domain.repository.CustomerRepository;
import com.sixpay.customer.management.domain.repository.CustomerSearchCriteria;
import com.sixpay.customer.management.domain.repository.CustomerSearchPage;
import com.sixpay.customer.verification.application.port.input.VerifyCustomerCommand;
import com.sixpay.customer.verification.application.port.input.VerifyCustomerResult;
import com.sixpay.customer.verification.application.port.input.VerifyCustomerUseCase;
import com.sixpay.customer.verification.application.port.output.BankingAccountAccessReference;
import com.sixpay.customer.verification.domain.model.AccountBindingFingerprint;
import com.sixpay.customer.verification.domain.model.CustomerIdentity;
import com.sixpay.customer.verification.domain.model.CustomerNiu;
import com.sixpay.customer.verification.domain.model.CustomerVerificationContext;
import com.sixpay.customer.verification.domain.model.CustomerVerificationId;
import com.sixpay.customer.verification.domain.model.CustomerVerificationSubject;
import com.sixpay.customer.verification.domain.model.FinancialInstitutionCode;
import com.sixpay.customer.verification.domain.model.VerificationOutcome;
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
    private final BankingCustomerLookupPort lookupPort;
    private final VerifyCustomerUseCase verifyCustomerUseCase;

    public CustomerManagementService(
            CustomerRepository repository,
            CustomerEnrollmentIdGenerator idGenerator,
            BankingCustomerLookupPort lookupPort,
            VerifyCustomerUseCase verifyCustomerUseCase
    ) {
        this.repository = Objects.requireNonNull(repository);
        this.idGenerator = Objects.requireNonNull(idGenerator);
        this.lookupPort = Objects.requireNonNull(lookupPort);
        this.verifyCustomerUseCase =
                Objects.requireNonNull(verifyCustomerUseCase);
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
    @Transactional(readOnly = true)
    public CustomerSearchPage search(
            CustomerSearchCriteria criteria
    ) {
        return repository.search(criteria);
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
        customer.updateProfile(
                legalName,
                email,
                phoneNumber,
                now
        );
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
        Objects.requireNonNull(command, "command is required");
        Objects.requireNonNull(now, "now is required");

        Customer customer = findById(customerId);

        BankingCustomerLookupPort.BankingCustomerProfile profile =
                lookupPort.lookup(
                        new BankingCustomerLookupPort.BankingCustomerLookupQuery(
                                customer.financialInstitutionCode(),
                                customer.niu().orElse(null),
                                customer.customerNumber().orElse(null),
                                command.accountReference(),
                                command.correlationId()
                        )
                );

        requireSameBankingCustomer(customer, profile);

        VerifyCustomerResult verification =
                verifyCustomerUseCase.verify(
                        new VerifyCustomerCommand(
                                new CustomerVerificationId(
                                        idGenerator.nextId()
                                ),
                                CustomerVerificationSubject.of(
                                        CustomerIdentity.of(
                                                CustomerNiu.of(
                                                        customer.niu()
                                                                .orElseThrow(
                                                                        () ->
                                                                                new CustomerDomainException(
                                                                                        "customer NIU is required for account verification"
                                                                                )
                                                                )
                                                ),
                                                customer.legalName()
                                        )
                                ),
                                FinancialInstitutionCode.of(
                                        customer.financialInstitutionCode()
                                ),
                                AccountBindingFingerprint.of(
                                        profile.account()
                                                .accountBindingFingerprint()
                                ),
                                BankingAccountAccessReference.of(
                                        profile.account()
                                                .bankingAccountAccessReference()
                                ),
                                CustomerVerificationContext.of(
                                        CorrelationId.of(
                                                command.correlationId()
                                        ),
                                        null
                                ),
                                now
                        )
                );

        requireFreshVerifiedEvidence(
                verification,
                now
        );

        customer.addBankAccount(
                CustomerBankAccount.create(
                        new CustomerBankAccountId(
                                idGenerator.nextId()
                        ),
                        customerId,
                        profile.account().accountReference(),
                        verification.accountBindingFingerprint().value(),
                        profile.account().maskedAccountIdentifier(),
                        profile.account().currency(),
                        profile.account().accountType(),
                        verification.observedAt()
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

    private static void requireSameBankingCustomer(
            Customer customer,
            BankingCustomerLookupPort.BankingCustomerProfile profile
    ) {
        if (!customer.financialInstitutionCode()
                .equals(profile.financialInstitutionCode())) {
            throw new CustomerDomainException(
                    "bank account belongs to another financial institution"
            );
        }

        if (!customer.bankingCustomerReference()
                .equals(profile.customerReference())) {
            throw new CustomerDomainException(
                    "bank account does not belong to enrolled customer"
            );
        }
    }

    private static void requireFreshVerifiedEvidence(
            VerifyCustomerResult verification,
            Instant operationTime
    ) {
        if (verification.outcome()
                != VerificationOutcome.VERIFIED) {
            throw new CustomerDomainException(
                    "bank account linking requires VERIFIED banking evidence"
            );
        }

        if (verification.validUntil() != null
                && verification.validUntil()
                        .isBefore(operationTime)) {
            throw new CustomerDomainException(
                    "bank account linking requires fresh banking evidence"
            );
        }

        if (verification.completedAt()
                .isBefore(verification.observedAt())) {
            throw new CustomerDomainException(
                    "banking verification timeline is invalid"
            );
        }
    }
}
