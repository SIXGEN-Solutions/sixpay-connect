package com.sixpay.customer.verification.application.service;

import com.sixpay.customer.verification.application.port.input.VerifyCustomerCommand;
import com.sixpay.customer.verification.application.port.input.VerifyCustomerResult;
import com.sixpay.customer.verification.application.port.input.VerifyCustomerUseCase;
import com.sixpay.customer.verification.application.port.output.BankingCustomerVerificationPort;
import com.sixpay.customer.verification.application.port.output.BankingVerificationResponse;
import com.sixpay.customer.verification.application.port.output.CustomerVerificationDomainEventPublisher;
import com.sixpay.customer.verification.application.port.output.CustomerVerificationEventIdGenerator;
import com.sixpay.customer.verification.application.port.output.CustomerVerificationRepository;
import com.sixpay.customer.verification.application.port.output.CustomerVerificationTimeProvider;
import com.sixpay.customer.verification.domain.event.CustomerVerificationDomainEvent;
import com.sixpay.customer.verification.domain.model.CustomerVerification;
import com.sixpay.customer.verification.domain.model.CustomerVerificationRequest;
import com.sixpay.customer.verification.domain.model.CustomerVerificationResult;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Framework-free Customer Verification application service.
 *
 * <p>The service owns orchestration only. It delegates banking communication,
 * persistence, event publication, time and event-ID generation to output
 * ports. It contains no HTTP, retry or Payment-specific logic.</p>
 */
public final class CustomerVerificationService
        implements VerifyCustomerUseCase {

    private final BankingCustomerVerificationPort bankingPort;
    private final CustomerVerificationRepository repository;
    private final CustomerVerificationDomainEventPublisher eventPublisher;
    private final CustomerVerificationEventIdGenerator eventIdGenerator;
    private final CustomerVerificationTimeProvider timeProvider;

    public CustomerVerificationService(
            BankingCustomerVerificationPort bankingPort,
            CustomerVerificationRepository repository,
            CustomerVerificationDomainEventPublisher eventPublisher,
            CustomerVerificationEventIdGenerator eventIdGenerator,
            CustomerVerificationTimeProvider timeProvider
    ) {
        this.bankingPort = Objects.requireNonNull(
                bankingPort,
                "bankingPort is required"
        );
        this.repository = Objects.requireNonNull(
                repository,
                "repository is required"
        );
        this.eventPublisher = Objects.requireNonNull(
                eventPublisher,
                "eventPublisher is required"
        );
        this.eventIdGenerator = Objects.requireNonNull(
                eventIdGenerator,
                "eventIdGenerator is required"
        );
        this.timeProvider = Objects.requireNonNull(
                timeProvider,
                "timeProvider is required"
        );
    }

    @Override
    public VerifyCustomerResult verify(
            VerifyCustomerCommand command
    ) {
        Objects.requireNonNull(command, "command is required");

        CustomerVerification verification =
                CustomerVerification.request(
                        toDomainRequest(command)
                );

        /*
         * Persist REQUESTED first so an exhausted technical call leaves a
         * traceable verification that may be resumed by application policy.
         */
        repository.save(verification);

        BankingVerificationResponse bankingResponse =
                bankingPort.verify(command.toBankingQuery());

        Instant completedAt = Objects.requireNonNull(
                timeProvider.now(),
                "timeProvider returned null"
        );

        CustomerVerificationResult result =
                verification.complete(
                        bankingResponse.toEvidence(),
                        Objects.requireNonNull(
                                eventIdGenerator.nextId(),
                                "eventIdGenerator returned null"
                        ),
                        completedAt
                );

        repository.save(verification);

        List<CustomerVerificationDomainEvent> events =
                verification.pullDomainEvents();

        if (!events.isEmpty()) {
            eventPublisher.publish(events);
        }

        return VerifyCustomerResult.from(
                verification.id(),
                result,
                command.accountBindingFingerprint()
        );
    }

    private static CustomerVerificationRequest toDomainRequest(
            VerifyCustomerCommand command
    ) {
        return new CustomerVerificationRequest(
                command.verificationId(),
                command.subject(),
                command.financialInstitutionCode(),
                command.accountBindingFingerprint(),
                command.context(),
                command.requestedAt()
        );
    }
}
