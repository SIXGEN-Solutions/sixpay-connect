package com.sixpay.bootstrap.integration.customer;

import com.sixpay.customer.observation.application.port.input.ObserveCustomerCommand;
import com.sixpay.customer.observation.application.port.input.ObserveCustomerResult;
import com.sixpay.customer.observation.application.port.input.ObserveCustomerUseCase;
import com.sixpay.customer.observation.domain.model.ObservedCustomerId;
import com.sixpay.payment.application.port.output.ObservedCustomerProjectionRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ObservedCustomerOutboxReplayIntegrationTest {

    @Test
    void replayedOutboxEventIsSuccessfulAndDoesNotIncrementProjection() {
        InMemoryIdempotentUseCase customer =
                new InMemoryIdempotentUseCase();

        var adapter =
                new ObservedCustomerProjectionModuleAdapter(
                        customer
                );

        ObservedCustomerProjectionRequest request = request();

        var first = adapter.project(request);
        var replay = adapter.project(request);

        assertEquals(
                com.sixpay.payment.application.port.output
                        .ObservedCustomerProjectionResult
                        .Disposition.APPLIED,
                first.disposition()
        );
        assertEquals(
                com.sixpay.payment.application.port.output
                        .ObservedCustomerProjectionResult
                        .Disposition.REPLAYED,
                replay.disposition()
        );
        assertEquals(1, customer.projectionVersion);
        assertEquals(1, customer.events.size());
    }

    private static ObservedCustomerProjectionRequest request() {
        return new ObservedCustomerProjectionRequest(
                UUID.fromString(
                        "11111111-1111-4111-8111-111111111111"
                ),
                UUID.fromString(
                        "7ed75090-8af7-4dfa-9b62-8e4dca73501a"
                ),
                "PAY-2026-000123",
                "M0123456",
                "Société ABC SARL",
                null,
                null,
                "AMPLITUDE",
                "v1:" + "a".repeat(64),
                "•••• 1234",
                new BigDecimal("15000.00"),
                "XAF",
                ObservedCustomerProjectionRequest
                        .ProjectionPaymentStatus.RECEIVED,
                null,
                Instant.parse("2026-08-03T20:00:00Z"),
                Instant.parse("2026-08-03T20:00:00Z"),
                Instant.parse("2026-08-03T20:00:01Z"),
                "c74e165f-df46-463e-a520-188e6df3e5ae"
        );
    }

    private static final class InMemoryIdempotentUseCase
            implements ObserveCustomerUseCase {

        private final Set<UUID> events = new HashSet<>();
        private long projectionVersion;

        @Override
        public ObserveCustomerResult observe(
                ObserveCustomerCommand command
        ) {
            boolean applied = events.add(
                    command.sourceEventId()
            );

            if (applied) {
                projectionVersion++;
            }

            return new ObserveCustomerResult(
                    ObservedCustomerId.of(
                            UUID.fromString(
                                    "901a3933-ae9e-4eb3-9fcf-f368a350a1db"
                            )
                    ),
                    command.sourceEventId(),
                    command.paymentId(),
                    applied
                            ? ObserveCustomerResult
                            .Disposition.APPLIED
                            : ObserveCustomerResult
                            .Disposition.REPLAYED,
                    projectionVersion,
                    command.observedAt()
            );
        }
    }
}
