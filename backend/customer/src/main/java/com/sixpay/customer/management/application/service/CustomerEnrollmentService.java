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
import com.sixpay.customer.management.domain.repository.CustomerRepository;
import com.sixpay.customer.verification.application.port.input.VerifyCustomerCommand;
import com.sixpay.customer.verification.application.port.output.BankingAccountAccessReference;
import com.sixpay.customer.verification.application.port.input.VerifyCustomerResult;
import com.sixpay.customer.verification.application.port.input.VerifyCustomerUseCase;
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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional
public class CustomerEnrollmentService
        implements EnrollCustomerUseCase {

    private final VerifyCustomerUseCase verifyCustomerUseCase;
    private final BankingCustomerLookupPort bankingLookupPort;
    private final CustomerEnrollmentIdGenerator idGenerator;
    private final CustomerEnrollmentTimeProvider timeProvider;
    private final CustomerRepository customerRepository;

    public CustomerEnrollmentService(
            VerifyCustomerUseCase verifyCustomerUseCase,
            BankingCustomerLookupPort bankingLookupPort,
            CustomerEnrollmentIdGenerator idGenerator,
            CustomerEnrollmentTimeProvider timeProvider,
            CustomerRepository customerRepository
    ) {
        this.verifyCustomerUseCase = Objects.requireNonNull(verifyCustomerUseCase);
        this.bankingLookupPort = Objects.requireNonNull(bankingLookupPort);
        this.idGenerator = Objects.requireNonNull(idGenerator);
        this.timeProvider = Objects.requireNonNull(timeProvider);
        this.customerRepository = Objects.requireNonNull(customerRepository);
    }

    @Override
    public EnrollCustomerResult enroll(EnrollCustomerCommand command) {
        Objects.requireNonNull(command, "command is required");

        Instant requestedAt = timeProvider.now();
        VerifyCustomerResult verification = verifyCustomerUseCase.verify(
                new VerifyCustomerCommand(
                        new CustomerVerificationId(idGenerator.nextId()),
                        CustomerVerificationSubject.of(CustomerIdentity.of(
                                command.niu() == null ? null : CustomerNiu.of(command.niu()),
                                null, null, command.customerNumber())),
                        FinancialInstitutionCode.of(command.financialInstitutionCode()),
                        null,
                        BankingAccountAccessReference.of(command.accountReference()),
                        CustomerVerificationContext.of(
                                CorrelationId.of(command.correlationId()), null),
                        requestedAt
                )
        );

        requireFreshVerifiedEvidence(verification, requestedAt);

        var identity = verification.identityOptional().orElseThrow(() ->
                new CustomerDomainException("verified banking identity is required"));
        var bankAccount = verification.accountOptional().orElseThrow(() ->
                new CustomerDomainException("verified banking account is required"));
        if (!command.financialInstitutionCode().equals(identity.financialInstitutionCode())
                || !identity.financialInstitutionCode().equals(bankAccount.financialInstitutionCode())
                || !verification.customerReference().equals(identity.customerReference())
                || !verification.customerReference().equals(bankAccount.customerReference())
                || !verification.accountReference().equals(bankAccount.accountReference())
                || !command.accountReference().equals(bankAccount.accountReference())
                || (command.niu() != null && !command.niu().equals(identity.niu()))
                || (command.customerNumber() != null
                        && !command.customerNumber().equals(identity.customerNumber()))) {
            throw new CustomerDomainException("banking enrollment identity or account mismatch");
        }
        if (bankAccount.retrievedAt().isAfter(verification.completedAt())
                || identity.retrievedAt().isAfter(verification.completedAt())) {
            throw new CustomerDomainException("banking enrollment evidence timeline is invalid");
        }

        // The VERIFIED banking response is the sole source for enrollment.
        if (customerRepository
                .existsByFinancialInstitutionCodeAndBankingCustomerReference(
                        identity.financialInstitutionCode(),
                        identity.customerReference()
                )) {
            throw new CustomerDomainException(
                    "customer is already enrolled input SIXPAY"
            );
        }

        CustomerId customerId =
                new CustomerId(idGenerator.nextId());

        CustomerBankAccount account =
                CustomerBankAccount.create(
                        new CustomerBankAccountId(idGenerator.nextId()),
                        customerId,
                        bankAccount.accountReference(),
                        stableAccountFingerprint(identity.financialInstitutionCode(),
                                identity.customerReference(), bankAccount.accountReference()),
                        bankAccount.maskedAccountIdentifier(),
                        bankAccount.currency(),
                        bankAccount.accountType(),
                        verification.observedAt()
                );

        Customer customer = Customer.create(
                customerId,
                identity.financialInstitutionCode(),
                identity.customerReference(),
                identity.customerNumber(),
                identity.niu(),
                identity.legalName(),
                identity.email(),
                identity.phoneNumber(),
                account,
                requestedAt
        );

        Customer persisted = customerRepository.save(customer);
        return new EnrollCustomerResult(persisted);
    }

    // Binding fingerprint is stable for the verified bank/customer/account tuple,
    // unlike the per-verification evidence fingerprint. No raw account value is stored.
    private static String stableAccountFingerprint(
            String institution, String customerReference, String accountReference) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String canonical = "SIXPAY-ACCOUNT-BINDING-v1\n"
                    + institution.length() + ":" + institution
                    + customerReference.length() + ":" + customerReference
                    + accountReference.length() + ":" + accountReference;
            return "v1:" + HexFormat.of().formatHex(
                    digest.digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
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
