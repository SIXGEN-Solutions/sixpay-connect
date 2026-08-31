package com.sixpay.customer.verification.application.service;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.customer.verification.application.exception.BankingVerificationUnavailableException;
import com.sixpay.customer.verification.application.port.input.VerifyCustomerCommand;
import com.sixpay.customer.verification.application.port.input.VerifyCustomerResult;
import com.sixpay.customer.verification.application.port.output.*;
import com.sixpay.customer.verification.domain.event.CustomerVerificationDomainEvent;
import com.sixpay.customer.verification.domain.model.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class CustomerVerificationServiceTest {

    private static final Instant REQUESTED_AT =
            Instant.parse("2026-08-03T18:00:00Z");
    private static final Instant OBSERVED_AT =
            Instant.parse("2026-08-03T18:00:01Z");
    private static final Instant COMPLETED_AT =
            Instant.parse("2026-08-03T18:00:02Z");
    private static final UUID EVENT_ID = UUID.fromString(
            "9dc8e15d-3e26-4cf1-9fd8-bc88aa39ac1e"
    );

    @Test
    void orchestratesRequestBankingCompletionPersistenceAndPublication() {
        RecordingRepository repository = new RecordingRepository();
        RecordingPublisher publisher = new RecordingPublisher();
        AtomicReference<BankingVerificationQuery> capturedQuery =
                new AtomicReference<>();

        BankingCustomerVerificationPort bankingPort = query -> {
            capturedQuery.set(query);
            return successfulBankingResponse();
        };

        CustomerVerificationService service =
                service(
                        bankingPort,
                        repository,
                        publisher
                );

        VerifyCustomerCommand command = command();
        VerifyCustomerResult result = service.verify(command);

        assertEquals(2, repository.saved.size());
        assertEquals(
                VerificationStatus.REQUESTED,
                repository.saved.get(0).status()
        );
        assertEquals(
                VerificationStatus.COMPLETED,
                repository.saved.get(1).status()
        );
        assertEquals(1, publisher.events.size());
        assertEquals(VerificationOutcome.VERIFIED, result.outcome());
        assertEquals(COMPLETED_AT, result.completedAt());
        assertEquals(
                command.context().correlationId(),
                capturedQuery.get().context().correlationId()
        );
        assertEquals(
                command.bankingAccountAccessReference(),
                capturedQuery.get().bankingAccountAccessReference()
        );
        assertEquals(
                command.accountBindingFingerprint(),
                result.accountBindingFingerprint()
        );
    }

    @Test
    void persistsRequestedStateButDoesNotPublishWhenBankingFails() {
        RecordingRepository repository = new RecordingRepository();
        RecordingPublisher publisher = new RecordingPublisher();

        BankingCustomerVerificationPort bankingPort = query -> {
            throw new BankingVerificationUnavailableException(
                    "Core Banking unavailable",
                    null
            );
        };

        CustomerVerificationService service =
                service(
                        bankingPort,
                        repository,
                        publisher
                );

        assertThrows(
                BankingVerificationUnavailableException.class,
                () -> service.verify(command())
        );

        assertEquals(1, repository.saved.size());
        assertEquals(
                VerificationStatus.REQUESTED,
                repository.saved.getFirst().status()
        );
        assertTrue(publisher.events.isEmpty());
    }

    @Test
    void neverPublishesBeforeCompletedAggregateIsSaved() {
        List<String> operations = new ArrayList<>();

        CustomerVerificationRepository repository =
                new CustomerVerificationRepository() {
                    @Override
                    public CustomerVerification save(
                            CustomerVerification verification
                    ) {
                        operations.add(
                                "save-" + verification.status()
                        );
                        return verification;
                    }

                    @Override
                    public Optional<CustomerVerification> findById(
                            CustomerVerificationId verificationId
                    ) {
                        return Optional.empty();
                    }
                };

        CustomerVerificationDomainEventPublisher publisher =
                events -> operations.add("publish");

        service(
                query -> successfulBankingResponse(),
                repository,
                publisher
        ).verify(command());

        assertEquals(
                List.of(
                        "save-REQUESTED",
                        "save-COMPLETED",
                        "publish"
                ),
                operations
        );
    }

    @Test
    void usesExplicitTimeAndEventIdProviders() {
        AtomicInteger timeCalls = new AtomicInteger();
        AtomicInteger eventIdCalls = new AtomicInteger();

        CustomerVerificationService service =
                new CustomerVerificationService(
                        query -> successfulBankingResponse(),
                        new RecordingRepository(),
                        events -> {
                        },
                        () -> {
                            eventIdCalls.incrementAndGet();
                            return EVENT_ID;
                        },
                        () -> {
                            timeCalls.incrementAndGet();
                            return COMPLETED_AT;
                        }
                );

        VerifyCustomerResult result = service.verify(command());

        assertEquals(1, timeCalls.get());
        assertEquals(1, eventIdCalls.get());
        assertEquals(COMPLETED_AT, result.completedAt());
    }

    private static CustomerVerificationService service(
            BankingCustomerVerificationPort bankingPort,
            CustomerVerificationRepository repository,
            CustomerVerificationDomainEventPublisher publisher
    ) {
        return new CustomerVerificationService(
                bankingPort,
                repository,
                publisher,
                () -> EVENT_ID,
                () -> COMPLETED_AT
        );
    }

    private static VerifyCustomerCommand command() {
        return new VerifyCustomerCommand(
                new CustomerVerificationId(
                        UUID.fromString(
                                "7ed75090-8af7-4dfa-9b62-8e4dca73501a"
                        )
                ),
                CustomerVerificationSubject.of(
                        CustomerIdentity.of(
                                CustomerNiu.of("M0123456"),
                                "Ada Lovelace"
                        )
                ),
                FinancialInstitutionCode.of("AMPLITUDE"),
                AccountBindingFingerprint.of(
                        "v1:" + "a".repeat(64)
                ),
                BankingAccountAccessReference.of(
                        "AMP-ACC-000123"
                ),
                CustomerVerificationContext.of(
                        CorrelationId.of("corr-4.4.2"),
                        null
                ),
                REQUESTED_AT
        );
    }

    private static BankingVerificationResponse
            successfulBankingResponse() {
        return BankingVerificationResponse.of(
                Arrays.stream(VerificationCheckType.values())
                        .map(VerificationCheck::passed)
                        .toList(),
                VerificationEvidenceFingerprint.of(
                        "v1:sha256:" + "b".repeat(64)
                ),
                OBSERVED_AT,
                OBSERVED_AT.plusSeconds(300),
                "CUST-0001",
                "AMP-ACC-000123",
                new VerifiedBankingIdentity(
                        "CUST-0001", "000001", "AMPLITUDE", "M0123456", "Ada Lovelace",
                        "+237690000001", "ada@example.test", "COMPLETE", List.of(), OBSERVED_AT, OBSERVED_AT
                ),
                new VerifiedBankingAccount(
                        "AMP-ACC-000123", "CUST-0001", "AMPLITUDE", "****0123",
                        "XAF", "CURRENT", "ACTIVE", List.of(), OBSERVED_AT
                )
        );
    }

    private static final class RecordingRepository
            implements CustomerVerificationRepository {

        private final List<CustomerVerification> saved =
                new ArrayList<>();

        @Override
        public CustomerVerification save(
                CustomerVerification verification
        ) {
            /*
             * Snapshot the aggregate state through reconstitution so the
             * first saved entry remains REQUESTED after later mutation.
             */
            saved.add(
                    CustomerVerification.reconstitute(
                            verification.request(),
                            verification.status(),
                            verification.result().orElse(null),
                            verification.updatedAt()
                    )
            );
            return verification;
        }

        @Override
        public Optional<CustomerVerification> findById(
                CustomerVerificationId verificationId
        ) {
            return saved.stream()
                    .filter(value -> value.id().equals(verificationId))
                    .reduce((first, second) -> second);
        }
    }

    private static final class RecordingPublisher
            implements CustomerVerificationDomainEventPublisher {

        private final List<CustomerVerificationDomainEvent> events =
                new ArrayList<>();

        @Override
        public void publish(
                List<CustomerVerificationDomainEvent> events
        ) {
            this.events.addAll(events);
        }
    }
}
