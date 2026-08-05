package com.sixpay.bootstrap.integration.customer.outbox;

import com.sixpay.payment.application.event.projection.ObservedCustomerProjectionEvent;
import com.sixpay.payment.application.event.projection.ObservedCustomerProjectionEventType;
import com.sixpay.payment.application.event.projection.ObservedCustomerProjectionPayload;
import com.sixpay.payment.application.event.projection.ProjectionPaymentStatus;
import com.sixpay.payment.application.port.output.ObservedCustomerProjectionResult;
import com.sixpay.payment.infrastructure.outbox.PaymentOutboxCompletionService;
import com.sixpay.payment.infrastructure.outbox.claim.PaymentOutboxClaim;
import com.sixpay.payment.infrastructure.outbox.claim.PaymentOutboxClaimService;
import com.sixpay.payment.infrastructure.outbox.serialization.PaymentOutboxEventDeserializer;
import com.sixpay.payment.infrastructure.outbox.serialization.UnknownPaymentOutboxEventTypeException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentObservedCustomerOutboxDispatcherTest {

    private static final Instant NOW =
            Instant.parse("2026-08-04T18:00:00Z");

    @Test
    void appliedReplayAndStaleResultsArePublishedOnlyAfterHandling() {
        for (ObservedCustomerProjectionResult.Disposition disposition
                : ObservedCustomerProjectionResult.Disposition.values()) {
            Fixture fixture = fixture(1);
            when(fixture.handler.handle(fixture.event)).thenReturn(
                    new ObservedCustomerProjectionResult(
                            fixture.event.eventId(),
                            disposition,
                            4
                    )
            );

            var results = fixture.dispatcher.dispatchAvailable(
                    NOW,
                    "worker-a",
                    10,
                    Duration.ofMinutes(2),
                    5,
                    Duration.ofSeconds(1),
                    Duration.ofMinutes(1)
            );

            assertEquals(
                    PaymentObservedCustomerOutboxResult.Outcome.PUBLISHED,
                    results.getFirst().outcome()
            );
            verify(fixture.handler).handle(fixture.event);
            verify(fixture.completion).markPublished(
                    fixture.claim.eventId(),
                    fixture.claim.claimedBy(),
                    NOW
            );
        }
    }

    @Test
    void temporaryInfrastructureFailureSchedulesRetry() {
        Fixture fixture = fixture(1);
        doThrow(new DataAccessResourceFailureException("temporary"))
                .when(fixture.handler)
                .handle(fixture.event);

        var result = fixture.dispatcher.dispatchAvailable(
                NOW,
                "worker-a",
                10,
                Duration.ofMinutes(2),
                5,
                Duration.ofSeconds(2),
                Duration.ofMinutes(1)
        ).getFirst();

        assertEquals(
                PaymentObservedCustomerOutboxResult.Outcome.RETRY_SCHEDULED,
                result.outcome()
        );
        assertEquals(NOW.plusSeconds(2), result.nextAttemptAt());
        verify(fixture.completion, never()).markPublished(
                fixture.claim.eventId(),
                fixture.claim.claimedBy(),
                NOW
        );
        verify(fixture.completion).markRetryableFailure(
                fixture.claim.eventId(),
                fixture.claim.claimedBy(),
                "temporary_persistence_failure",
                NOW,
                NOW.plusSeconds(2)
        );
    }

    @Test
    void unknownContractIsDeadLetteredWithoutCallingCustomer() {
        Fixture fixture = fixture(1);
        when(fixture.deserializer.deserialize(fixture.claim.payload()))
                .thenThrow(
                        new UnknownPaymentOutboxEventTypeException(
                                "payment.unknown"
                        )
                );

        var result = fixture.dispatcher.dispatchAvailable(
                NOW,
                "worker-a",
                10,
                Duration.ofMinutes(2),
                5,
                Duration.ofSeconds(1),
                Duration.ofMinutes(1)
        ).getFirst();

        assertEquals(
                PaymentObservedCustomerOutboxResult.Outcome.DEAD_LETTERED,
                result.outcome()
        );
        verify(fixture.handler, never()).handle(fixture.event);
        verify(fixture.completion).markDead(
                fixture.claim.eventId(),
                fixture.claim.claimedBy(),
                "unknown_event_type",
                NOW
        );
    }

    @Test
    void exhaustedRetryableFailureBecomesDead() {
        Fixture fixture = fixture(5);
        doThrow(new DataAccessResourceFailureException("temporary"))
                .when(fixture.handler)
                .handle(fixture.event);

        var result = fixture.dispatcher.dispatchAvailable(
                NOW,
                "worker-a",
                10,
                Duration.ofMinutes(2),
                5,
                Duration.ofSeconds(1),
                Duration.ofMinutes(1)
        ).getFirst();

        assertEquals(
                PaymentObservedCustomerOutboxResult.Outcome.DEAD_LETTERED,
                result.outcome()
        );
        verify(fixture.completion).markDead(
                fixture.claim.eventId(),
                fixture.claim.claimedBy(),
                "temporary_persistence_failure",
                NOW
        );
    }

    private static Fixture fixture(int attempt) {
        PaymentOutboxClaimService claimService =
                mock(PaymentOutboxClaimService.class);
        PaymentOutboxEventDeserializer deserializer =
                mock(PaymentOutboxEventDeserializer.class);
        PaymentObservedCustomerOutboxHandler handler =
                mock(PaymentObservedCustomerOutboxHandler.class);
        PaymentOutboxCompletionService completion =
                mock(PaymentOutboxCompletionService.class);

        ObservedCustomerProjectionEvent event = event();
        PaymentOutboxClaim claim = new PaymentOutboxClaim(
                event.eventId(),
                event.paymentId(),
                event.aggregateType(),
                "payment.observation-projection",
                event.eventVersion(),
                event.correlationId(),
                "{\"event\":\"projection\"}",
                event.occurredAt(),
                attempt,
                NOW.minusSeconds(1),
                "worker-a"
        );

        when(claimService.claimAvailableByEventType(
                "payment.observation-projection",
                NOW,
                NOW.minus(Duration.ofMinutes(2)),
                10,
                "worker-a"
        )).thenReturn(List.of(claim));
        when(deserializer.deserialize(claim.payload()))
                .thenReturn(event);

        var dispatcher = new PaymentObservedCustomerOutboxDispatcher(
                claimService,
                deserializer,
                handler,
                new PaymentObservedCustomerOutboxFailureClassifier(),
                completion
        );

        return new Fixture(
                dispatcher,
                deserializer,
                handler,
                completion,
                claim,
                event
        );
    }

    private static ObservedCustomerProjectionEvent event() {
        Instant createdAt = Instant.parse("2026-08-04T17:55:00Z");

        return ObservedCustomerProjectionEvent.versionOne(
                UUID.fromString("11111111-1111-4111-8111-111111111111"),
                UUID.fromString("7ed75090-8af7-4dfa-9b62-8e4dca73501a"),
                8,
                ObservedCustomerProjectionEventType.PAYMENT_REJECTED,
                new ObservedCustomerProjectionPayload(
                        "PAY-2026-000123",
                        "M0123456",
                        "Société ABC SARL",
                        "***-***-1234",
                        "a***@example.com",
                        "AMPLITUDE",
                        "v1:" + "a".repeat(64),
                        "•••• 1234",
                        new BigDecimal("15000.00"),
                        "XAF",
                        ProjectionPaymentStatus.REJECTED,
                        "ACCOUNT_NOT_FOUND",
                        createdAt,
                        NOW
                ),
                "c74e165f-df46-463e-a520-188e6df3e5ae",
                NOW
        );
    }

    private record Fixture(
            PaymentObservedCustomerOutboxDispatcher dispatcher,
            PaymentOutboxEventDeserializer deserializer,
            PaymentObservedCustomerOutboxHandler handler,
            PaymentOutboxCompletionService completion,
            PaymentOutboxClaim claim,
            ObservedCustomerProjectionEvent event
    ) {
    }
}
