package com.sixpay.customer.management.application.service;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.customer.management.application.port.input.EnrollCustomerCommand;
import com.sixpay.customer.management.application.port.input.EnrollCustomerResult;
import com.sixpay.customer.management.application.port.input.EnrollCustomerUseCase;
import com.sixpay.customer.management.application.port.output.BankingCustomerLookupPort;
import com.sixpay.customer.management.application.port.output.CustomerEnrollmentIdGenerator;
import com.sixpay.customer.management.application.port.output.CustomerEnrollmentTimeProvider;
import com.sixpay.customer.management.domain.exception.CustomerDomainException;
import com.sixpay.customer.management.domain.model.Customer;
import com.sixpay.customer.management.domain.model.CustomerBankAccount;
import com.sixpay.customer.management.domain.model.CustomerBankAccountId;
import com.sixpay.customer.management.domain.model.CustomerId;
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

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
public final class CustomerEnrollmentService
        implements EnrollCustomerUseCase {

    private final BankingCustomerLookupPort lookupPort;
    private final VerifyCustomerUseCase verifyCustomerUseCase;
    private final CustomerEnrollmentIdGenerator idGenerator;
    private final CustomerEnrollmentTimeProvider timeProvider;

    public CustomerEnrollmentService(
            BankingCustomerLookupPort lookupPort,
            VerifyCustomerUseCase verifyCustomerUseCase,
            CustomerEnrollmentIdGenerator idGenerator,
            CustomerEnrollmentTimeProvider timeProvider
    ) {
        this.lookupPort = Objects.requireNonNull(lookupPort);
        this.verifyCustomerUseCase = Objects.requireNonNull(verifyCustomerUseCase);
        this.idGenerator = Objects.requireNonNull(idGenerator);
        this.timeProvider = Objects.requireNonNull(timeProvider);
    }

    @Override
    public EnrollCustomerResult enroll(EnrollCustomerCommand command) {
        Objects.requireNonNull(command, "command is required");

        var profile = lookupPort.lookup(
                new BankingCustomerLookupPort.BankingCustomerLookupQuery(
                        command.financialInstitutionCode(),
                        command.niu(),
                        command.customerNumber(),
                        command.accountReference(),
                        command.correlationId()
                )
        );

        Instant requestedAt = timeProvider.now();

        VerifyCustomerResult verification =
                verifyCustomerUseCase.verify(
                        new VerifyCustomerCommand(
                                new CustomerVerificationId(idGenerator.nextId()),
                                CustomerVerificationSubject.of(
                                        CustomerIdentity.of(
                                                CustomerNiu.of(profile.niu()),
                                                profile.legalName()
                                        )
                                ),
                                FinancialInstitutionCode.of(
                                        profile.financialInstitutionCode()
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
                                requestedAt
                        )
                );

        requireFreshVerifiedEvidence(verification, requestedAt);

        CustomerId customerId =
                new CustomerId(idGenerator.nextId());

        CustomerBankAccount account =
                CustomerBankAccount.create(
                        new CustomerBankAccountId(idGenerator.nextId()),
                        customerId,
                        profile.account().accountReference(),
                        verification.accountBindingFingerprint().value(),
                        profile.account().maskedAccountIdentifier(),
                        profile.account().currency(),
                        profile.account().accountType(),
                        verification.observedAt()
                );

        Customer customer = Customer.create(
                customerId,
                profile.financialInstitutionCode(),
                profile.customerReference(),
                profile.customerNumber(),
                profile.niu(),
                profile.legalName(),
                profile.email(),
                profile.phoneNumber(),
                account,
                requestedAt
        );

        return new EnrollCustomerResult(customer);
    }

    private static void requireFreshVerifiedEvidence(
            VerifyCustomerResult verification,
            Instant enrollmentTime
    ) {
        if (verification.outcome() != VerificationOutcome.VERIFIED) {
            throw new CustomerDomainException(
                    "customer enrollment requires VERIFIED banking evidence"
            );
        }

        if (verification.validUntil() != null
                && verification.validUntil().isBefore(enrollmentTime)) {
            throw new CustomerDomainException(
                    "customer enrollment requires fresh banking evidence"
            );
        }

        if (verification.completedAt().isBefore(
                verification.observedAt()
        )) {
            throw new CustomerDomainException(
                    "banking verification timeline is invalid"
            );
        }
    }
}
