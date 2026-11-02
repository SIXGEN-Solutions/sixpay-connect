package com.sixpay.customer.verification.application.service;

import com.sixpay.customer.verification.application.port.input.VerifyCustomerCommand;
import com.sixpay.customer.verification.application.port.input.VerifyCustomerResult;
import com.sixpay.customer.verification.application.port.input.VerifyCustomerUseCase;
import com.sixpay.customer.verification.application.port.output.BankingCustomerVerificationPort;
import com.sixpay.customer.verification.application.port.output.BankingVerificationResponse;
import com.sixpay.customer.verification.application.port.output.CustomerVerificationDomainEventPublisher;
import com.sixpay.customer.verification.application.port.output.CustomerVerificationEventIdGenerator;
import com.sixpay.customer.verification.application.port.output.CustomerVerificationTimeProvider;
import com.sixpay.customer.verification.domain.event.CustomerVerificationDomainEvent;
import com.sixpay.customer.verification.domain.model.AccountBindingFingerprint;
import com.sixpay.customer.verification.domain.model.CustomerVerification;
import com.sixpay.customer.verification.domain.model.VerificationOutcome;
import com.sixpay.customer.verification.domain.model.CustomerVerificationRequest;
import com.sixpay.customer.verification.domain.model.CustomerVerificationResult;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/**
 * Framework-free Customer Verification application service.
 *
 * <p>The service owns orchestration only. It delegates banking communication,
 * event publication, time and event-ID generation to output ports. It contains
 * no HTTP, retry, persistence or Payment-specific logic.</p>
 */
public final class CustomerVerificationService
        implements VerifyCustomerUseCase {

    private final BankingCustomerVerificationPort bankingPort;
    private final CustomerVerificationDomainEventPublisher eventPublisher;
    private final CustomerVerificationEventIdGenerator eventIdGenerator;
    private final CustomerVerificationTimeProvider timeProvider;

    public CustomerVerificationService(
            BankingCustomerVerificationPort bankingPort,
            CustomerVerificationDomainEventPublisher eventPublisher,
            CustomerVerificationEventIdGenerator eventIdGenerator,
            CustomerVerificationTimeProvider timeProvider
    ) {
        this.bankingPort = Objects.requireNonNull(
                bankingPort,
                "bankingPort is required"
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

        BankingVerificationResponse bankingResponse =
                bankingPort.verify(command.toBankingQuery());

        AccountBindingFingerprint resolvedBinding =
                resolveAccountBinding(command, bankingResponse);

        CustomerVerification verification =
                CustomerVerification.request(
                        toDomainRequest(command, resolvedBinding)
                );

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

        List<CustomerVerificationDomainEvent> events =
                verification.pullDomainEvents();

        if (!events.isEmpty()) {
            eventPublisher.publish(events);
        }

        return VerifyCustomerResult.from(
                verification.id(),
                result,
                resolvedBinding,
                bankingResponse
        );
    }

    private static CustomerVerificationRequest toDomainRequest(
            VerifyCustomerCommand command,
            AccountBindingFingerprint resolvedBinding
    ) {
        return new CustomerVerificationRequest(
                command.verificationId(),
                command.subject(),
                command.financialInstitutionCode(),
                resolvedBinding,
                command.context(),
                command.requestedAt()
        );
    }
    /**
     * The banking triplet is authoritative only after positive verification.
     * Never invent a binding from an unresolved or rejected response.
     */
    private static AccountBindingFingerprint resolveAccountBinding(
            VerifyCustomerCommand command,
            BankingVerificationResponse bankingResponse
    ) {
        if (command.accountBindingFingerprint() != null) {
            return command.accountBindingFingerprint();
        }
        // No synthetic binding is valid for a negative or indeterminate result.
        // This branch deliberately fails closed until the domain event lifecycle
        // for unresolved bindings is separately approved.
        if (bankingResponse.customerReference() == null
                || bankingResponse.accountReference() == null
                || bankingResponse.identity() == null
                || bankingResponse.account() == null) {
            throw new IllegalStateException(
                    "Cannot complete customer verification without resolved account binding");
        }
        var institution = bankingResponse.identity().financialInstitutionCode();
        var customer = bankingResponse.customerReference();
        var account = bankingResponse.accountReference();
        if (institution == null || institution.isBlank()
                || customer.isBlank() || account.isBlank()
                || !institution.equals(command.financialInstitutionCode().value())
                || !customer.equals(bankingResponse.identity().customerReference())
                || !customer.equals(bankingResponse.account().customerReference())
                || !account.equals(bankingResponse.account().accountReference())) {
            throw new IllegalStateException(
                    "Verified banking references cannot establish an account binding");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String canonical = "SIXPAY-ACCOUNT-BINDING-v1\n"
                    + institution.length() + ":" + institution
                    + customer.length() + ":" + customer
                    + account.length() + ":" + account;
            return AccountBindingFingerprint.of("v1:" + HexFormat.of().formatHex(
                    digest.digest(canonical.getBytes(StandardCharsets.UTF_8))));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

}
