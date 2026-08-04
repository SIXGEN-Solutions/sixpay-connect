package com.sixpay.bootstrap.integration.customer;

import com.sixpay.customer.observation.application.port.input.ObserveCustomerResult;
import com.sixpay.customer.observation.application.port.input.ObserveCustomerUseCase;
import com.sixpay.customer.observation.domain.model.ObservedCustomerId;
import com.sixpay.payment.application.port.output.ObservedCustomerProjectionRequest;
import com.sixpay.payment.application.port.output.ObservedCustomerProjectionResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ObservedCustomerProjectionModuleAdapterTest {

    @Test
    void mapsPaymentContractToCustomerObservationAndPreservesEventIdentity() {
        AtomicReference<com.sixpay.customer.observation
                .application.port.input.ObserveCustomerCommand> captured =
                new AtomicReference<>();

        ObserveCustomerUseCase useCase = command -> {
            captured.set(command);
            return new ObserveCustomerResult(
                    ObservedCustomerId.of(
                            UUID.fromString(
                                    "901a3933-ae9e-4eb3-9fcf-f368a350a1db"
                            )
                    ),
                    command.sourceEventId(),
                    command.paymentId(),
                    ObserveCustomerResult.Disposition.APPLIED,
                    4,
                    command.observedAt()
            );
        };

        var adapter =
                new ObservedCustomerProjectionModuleAdapter(
                        useCase
                );

        var request = request();
        ObservedCustomerProjectionResult result =
                adapter.project(request);

        assertEquals(
                request.sourceEventId(),
                captured.get().sourceEventId()
        );
        assertEquals(
                request.correlationId(),
                captured.get().correlationId()
        );
        assertEquals(
                request.accountBindingFingerprint(),
                captured.get().accountBindingFingerprint()
        );
        assertEquals(
                request.maskedAccountReference(),
                captured.get().maskedAccountReference()
        );
        assertEquals(
                ObservedCustomerProjectionResult.Disposition.APPLIED,
                result.disposition()
        );
        assertEquals(4, result.projectionVersion());
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
                        .ProjectionPaymentStatus.REJECTED,
                "ACCOUNT_NOT_FOUND",
                Instant.parse("2026-08-03T20:00:00Z"),
                Instant.parse("2026-08-03T20:05:00Z"),
                Instant.parse("2026-08-03T20:05:01Z"),
                "c74e165f-df46-463e-a520-188e6df3e5ae"
        );
    }
}
